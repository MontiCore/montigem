import { ChangeDetectionStrategy, Component, TemplateRef, ViewChild, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import qrcode from 'qrcode-generator';
import { config, ShareButtonComponent } from 'home/ShareButtonComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, MatButtonModule, MatDialogModule],
  selector: config.selector,
  templateUrl: config.templateUrl,
  styleUrls: config.styleUrls,
})
export class ShareButton extends ShareButtonComponent {
  @ViewChild('shareDialog', { static: true })
  shareDialog!: TemplateRef<unknown>;

  shareUrl = '';
  qrCodeUrl = '';
  copyMessage = '';
  copying = false;
  private readonly dialog = inject(MatDialog);
  private dialogRef?: MatDialogRef<unknown>;

  openShare(): void {
    this.shareUrl = window.location.href;
    this.copyMessage = '';
    this.copying = false;
    this.qrCodeUrl = '';
    try {
      const code = qrcode(0, 'M');
      code.addData(this.shareUrl);
      code.make();
      // Four modules of white space on every side make the code easier to scan.
      this.qrCodeUrl = code.createDataURL(6, 24);
    } catch {
      this.copyMessage = 'The QR code could not be generated. You can still copy the link.';
    }
    this.dialogRef = this.dialog.open(this.shareDialog, {
      width: '420px',
      maxWidth: 'calc(100vw - 24px)',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
    });
  }

  async copyLink(link: HTMLInputElement): Promise<void> {
    this.copying = true;
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(this.shareUrl);
      } else {
        this.copyWithSelection(link);
      }
      this.copyMessage = 'Link copied!';
    } catch {
      try {
        this.copyWithSelection(link);
        this.copyMessage = 'Link copied!';
      } catch {
        this.copyMessage = 'Select and copy the link above.';
      }
    } finally {
      this.copying = false;
    }
  }

  private copyWithSelection(link: HTMLInputElement): void {
    link.focus();
    link.select();
    if (!document.execCommand('copy')) {
      throw new Error('Clipboard access unavailable');
    }
  }

  override ngOnDestroy(): void {
    this.dialogRef?.close();
    super.ngOnDestroy();
  }
}
