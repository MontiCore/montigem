/* (c) https://github.com/MontiCore/monticore */

import { ChangeDetectionStrategy, Component, HostBinding, ViewEncapsulation } from '@angular/core';
import { GemMarkdown } from '@umlp/advanced';
import { GemIcon } from '@umlp/basic';
import { config, GemChatMessageComponent } from '../../../../../../build/gui2java2ts/mc/fenix/chat/GemChatMessageComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, GemIcon, GemMarkdown],
  selector: config.selector,
  styleUrls: config.styleUrls,
  // `sources` always contains the iteration over the sources, even if it is empty. Hence, the
  // toggle is hidden based on the rendered sources, which are not part of this component's view.
  styles: [`
    gem-chat-message { display: flex; flex: 0 0 auto; }
    gem-chat-message.gem-chat-message-user { justify-content: flex-end; }
    gem-chat-message:not(:has(gem-chat-source)) .gem-chat-message-sources-toggle { display: none; }
  `],
  encapsulation: ViewEncapsulation.None,
  templateUrl: config.templateUrl,
})
export class GemChatMessage extends GemChatMessageComponent {
  override _timestamp: string = '';
  @HostBinding('class.gem-chat-message-user')
  override _user: boolean = false;
  override _sourcesLabel: string = 'Sources';

  sourcesExpanded = false;
}
