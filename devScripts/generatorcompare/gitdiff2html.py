#!/usr/bin/env python3
"""Render `git diff` (incl. --no-index) output as a static, JS-free HTML page.

Usage:
  git diff --no-index old/ new/ | python3 gitdiff2html.py > diff.html
  python3 gitdiff2html.py changes.diff --split -o diff.html
"""
import argparse, collections, difflib, html, re, sys

HUNK_RE = re.compile(r"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@(.*)")

CSS = """
:root{--add:#e6ffec;--addw:#abf2bc;--del:#ffebe9;--delw:#ffc0c0;--hunk:#ddf4ff;
--num:#6e7781;--border:#d0d7de;--head:#f6f8fa;--fg:#1f2328;--bg:#fff}
@media (prefers-color-scheme:dark){:root{--add:#12261e;--addw:#1f6f3a;--del:#2d1214;
--delw:#8e1519;--hunk:#121d2f;--num:#8b949e;--border:#30363d;--head:#161b22;--fg:#e6edf3;--bg:#0d1117}}
*{box-sizing:border-box}
body{margin:0;padding:24px;font:14px/1.5 -apple-system,Segoe UI,Helvetica,Arial,sans-serif;color:var(--fg);background:var(--bg)}
h1{font-size:18px;margin:0 0 12px}
.summary{margin:0 0 24px;padding:12px 16px;border:1px solid var(--border);border-radius:6px;background:var(--head)}
.summary ul{margin:6px 0 0;padding-left:20px}
.summary a{color:inherit}
.stat{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12px;margin-left:6px}
.a{color:#1a7f37}.d{color:#cf222e}
details.file{border:1px solid var(--border);border-radius:6px;margin-bottom:20px;overflow:hidden}
details.file>summary{position:sticky;top:0;z-index:1;cursor:pointer;padding:8px 12px;background:var(--head);
border-bottom:1px solid var(--border);font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:13px}
.badge{font-size:11px;padding:1px 6px;border-radius:10px;border:1px solid var(--border);margin-left:6px}
.scroll{overflow-x:auto}
table{border-collapse:collapse;width:100%;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12px;line-height:20px}
td{padding:0 10px;vertical-align:top}
td.n{width:1%;min-width:50px;text-align:right;color:var(--num);user-select:none;white-space:nowrap}
td.c{white-space:pre-wrap;word-break:break-all;width:49%}
table.unified td.c{width:auto}
td.s{width:1%;user-select:none;padding:0 2px 0 6px;white-space:pre}
tr.add td{background:var(--add)} tr.del td{background:var(--del)}
td.add{background:var(--add)} td.del{background:var(--del)} td.empty{background:var(--head)}
ins{background:var(--addw);text-decoration:none;border-radius:2px}
del{background:var(--delw);text-decoration:none;border-radius:2px}
tr.hunk td{background:var(--hunk);color:var(--num);padding:4px 10px}
tr.side th{padding:4px 10px;text-align:left;font-weight:600;background:var(--head);border-bottom:1px solid var(--border)}
tr.side th+th{border-left:1px solid var(--border)}
h1 .a,h1 .d{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace}
.meta{padding:12px;color:var(--num);font-style:italic}
"""

def parse(text):
    files, cur = [], None
    left_o = left_n = 0          # lines still expected in the current hunk
    for raw in text.splitlines():
        if left_o > 0 or left_n > 0:  # inside a hunk: trust the counts
            t = raw[:1]
            if t == "-": left_o -= 1
            elif t == "+": left_n -= 1
            elif t in (" ", ""): left_o -= 1; left_n -= 1; raw = " " + raw[1:]
            cur["hunks"][-1]["lines"].append(raw)
            continue
        if raw.startswith("\\") and cur and cur["hunks"]:
            cur["hunks"][-1]["lines"].append(raw); continue
        if raw.startswith("diff --git ") or (raw.startswith("--- ") and (cur is None or cur["hunks"])):
            cur = {"old": None, "new": None, "hunks": [], "meta": [], "header": raw}
            files.append(cur)
        if cur is None:
            continue
        if raw.startswith("--- "): cur["old"] = raw[4:]; continue
        if raw.startswith("+++ "): cur["new"] = raw[4:]; continue
        m = HUNK_RE.match(raw)
        if m:
            left_o = int(m[2]) if m[2] is not None else 1
            left_n = int(m[4]) if m[4] is not None else 1
            cur["hunks"].append({"o": int(m[1]), "n": int(m[3]), "lines": [], "head": raw})
            continue
        if not raw.startswith("diff --git "):
            cur["meta"].append(raw)
    return files

def clean(p):
    if p is None: return None
    p = p.split("\t")[0]
    if p == "/dev/null": return None
    if p.startswith(("a/", "b/")): p = p[2:]
    return p

def find_roots(files):
    """Top level dirs of a `git diff --no-index <old dir> <new dir>`, else (None, None).

    They are what is left of the two paths of a file in front of the part both have in common,
    e.g. dev_1a2b3c4 and feature_5d6e7f8 for
    dev_1a2b3c4/backend/A.java and feature_5d6e7f8/backend/A.java.
    """
    pairs = collections.Counter()
    for f in files:
        o, n = clean(f["old"]), clean(f["new"])
        if not o or not n:
            continue
        if o == n:  # a diff inside of a repository, there are no top level dirs
            return None, None
        op, np = o.split("/"), n.split("/")
        k = 0
        while k < min(len(op), len(np)) - 1 and op[-1 - k] == np[-1 - k]: k += 1
        if k:
            pairs[("/".join(op[:-k]), "/".join(np[:-k]))] += 1
    return pairs.most_common(1)[0][0] if pairs else (None, None)

def strip_root(p, root):
    return p[len(root) + 1:] if p and root and p.startswith(root + "/") else p

def file_name(f, roots=(None, None)):
    o, n = clean(f["old"]), clean(f["new"])
    if not o and not n:
        m = re.match(r"diff --git a/(.*) b/(.*)", f["header"])
        if m: o, n = m[1], m[2]
    # --no-index old/ new/: same relative path under different roots isn't a rename
    o, n = strip_root(o, roots[0]), strip_root(n, roots[1])
    if o and n and o != n:
        return f"{o} → {n}", "renamed"
    if not o and n: return n, "added"
    if o and not n: return o, "deleted"
    return n or o or "?", None

def intraline(a, b):
    sm = difflib.SequenceMatcher(None, a, b, autojunk=False)
    if sm.ratio() < 0.4:
        return html.escape(a), html.escape(b)
    oa, ob = [], []
    for op, i1, i2, j1, j2 in sm.get_opcodes():
        x, y = html.escape(a[i1:i2]), html.escape(b[j1:j2])
        if op == "equal": oa.append(x); ob.append(y)
        else:
            if x: oa.append(f"<del>{x}</del>")
            if y: ob.append(f"<ins>{y}</ins>")
    return "".join(oa), "".join(ob)

def rows(hunk):
    """Yield ('ctx', o, n, text) or ('chg', [(o,html)], [(n,html)])."""
    o, n = hunk["o"], hunk["n"]
    lines = [l for l in hunk["lines"] if not l.startswith("\\")]
    i = 0
    while i < len(lines):
        l = lines[i]
        if l[:1] == " ":
            yield ("ctx", o, n, html.escape(l[1:])); o += 1; n += 1; i += 1; continue
        dels, adds = [], []
        while i < len(lines) and lines[i][:1] == "-": dels.append(lines[i][1:]); i += 1
        while i < len(lines) and lines[i][:1] == "+": adds.append(lines[i][1:]); i += 1
        hd, ha = [html.escape(x) for x in dels], [html.escape(x) for x in adds]
        for k in range(min(len(dels), len(adds))):
            hd[k], ha[k] = intraline(dels[k], adds[k])
        dl = [(o + k, h) for k, h in enumerate(hd)]; o += len(dels)
        al = [(n + k, h) for k, h in enumerate(ha)]; n += len(adds)
        yield ("chg", dl, al)

def render_unified(f, roots=(None, None)):
    out = ['<table class="unified">']
    if roots[0] and roots[1]:
        out.append(f'<tr class="side"><th colspan="4"><span class="d">− {html.escape(roots[0])}</span>'
                   f' &nbsp; <span class="a">+ {html.escape(roots[1])}</span></th></tr>')
    for h in f["hunks"]:
        out.append(f'<tr class="hunk"><td class="n"></td><td class="n"></td><td colspan="2">{html.escape(h["head"])}</td></tr>')
        for r in rows(h):
            if r[0] == "ctx":
                out.append(f'<tr><td class="n">{r[1]}</td><td class="n">{r[2]}</td><td class="s"> </td><td class="c">{r[3]}</td></tr>')
            else:
                for no, t in r[1]:
                    out.append(f'<tr class="del"><td class="n">{no}</td><td class="n"></td><td class="s">-</td><td class="c">{t}</td></tr>')
                for nn, t in r[2]:
                    out.append(f'<tr class="add"><td class="n"></td><td class="n">{nn}</td><td class="s">+</td><td class="c">{t}</td></tr>')
    out.append("</table>")
    return "\n".join(out)

def render_split(f, roots=(None, None)):
    out = ['<table class="split">']
    if roots[0] and roots[1]:
        out.append(f'<tr class="side"><th colspan="3">{html.escape(roots[0])}</th>'
                   f'<th colspan="3">{html.escape(roots[1])}</th></tr>')
    for h in f["hunks"]:
        out.append(f'<tr class="hunk"><td class="n"></td><td colspan="5">{html.escape(h["head"])}</td></tr>')
        for r in rows(h):
            if r[0] == "ctx":
                out.append(f'<tr><td class="n">{r[1]}</td><td class="s"> </td><td class="c">{r[3]}</td>'
                           f'<td class="n">{r[2]}</td><td class="s"> </td><td class="c">{r[3]}</td></tr>')
            else:
                dl, al = r[1], r[2]
                for k in range(max(len(dl), len(al))):
                    if k < len(dl): L = f'<td class="n del">{dl[k][0]}</td><td class="s del">-</td><td class="c del">{dl[k][1]}</td>'
                    else: L = '<td class="n empty"></td><td class="s empty"></td><td class="c empty"></td>'
                    if k < len(al): R = f'<td class="n add">{al[k][0]}</td><td class="s add">+</td><td class="c add">{al[k][1]}</td>'
                    else: R = '<td class="n empty"></td><td class="s empty"></td><td class="c empty"></td>'
                    out.append(f"<tr>{L}{R}</tr>")
    out.append("</table>")
    return "\n".join(out)

def count(f):
    a = d = 0
    for h in f["hunks"]:
        for l in h["lines"]:
            if l.startswith("+"): a += 1
            elif l.startswith("-"): d += 1
    return a, d

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("input", nargs="?", help="diff file (default: stdin)")
    ap.add_argument("-o", "--output", help="output HTML file (default: stdout)")
    ap.add_argument("-s", "--split", action="store_true", help="side-by-side view")
    ap.add_argument("-t", "--title", default="Diff",
                    help="title, the top level dirs of a --no-index diff are appended (default: Diff)")
    ap.add_argument("--collapsed", action="store_true", help="start with files collapsed")
    a = ap.parse_args()

    text = open(a.input, encoding="utf-8", errors="replace").read() if a.input else sys.stdin.read()
    files = parse(text)
    roots = find_roots(files)
    title, title_html = a.title, html.escape(a.title)
    if roots[0] and roots[1]:
        title += f": {roots[0]} {roots[1]}"
        title_html += f': <span class="d">{html.escape(roots[0])}</span> <span class="a">{html.escape(roots[1])}</span>'
    ta = td = 0
    items, bodies = [], []
    for i, f in enumerate(files):
        name, kind = file_name(f, roots)
        ad, dd = count(f); ta += ad; td += dd
        badge = f'<span class="badge">{kind}</span>' if kind else ""
        stat = f'<span class="stat"><span class="a">+{ad}</span> <span class="d">−{dd}</span></span>'
        items.append(f'<li><a href="#f{i}">{html.escape(name)}</a>{stat}</li>')
        if f["hunks"]:
            body = render_split(f, roots) if a.split else render_unified(f, roots)
        else:
            body = '<div class="meta">' + (html.escape(" · ".join(f["meta"])) or "No textual changes") + "</div>"
        bodies.append(f'<details class="file" id="f{i}"{"" if a.collapsed else " open"}>'
                      f'<summary>{html.escape(name)}{badge}{stat}</summary><div class="scroll">{body}</div></details>')

    page = f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)}</title><style>{CSS}</style></head><body>
<h1>{title_html}</h1>
<div class="summary"><strong>{len(files)} file{'s' if len(files)!=1 else ''} changed</strong>
<span class="stat"><span class="a">+{ta}</span> <span class="d">−{td}</span></span>
<ul>{''.join(items)}</ul></div>
{''.join(bodies)}
</body></html>"""
    if a.output:
        open(a.output, "w", encoding="utf-8").write(page)
    else:
        sys.stdout.write(page)

if __name__ == "__main__":
    main()
