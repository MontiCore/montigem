/* (c) https://github.com/MontiCore/monticore */

import { Navigation } from 'navigation/Navigation';
import { provideMonacoEditor } from 'ngx-monaco-editor-v2';

export const config = {
  declarations: [],
  exports: [],
  imports: [
    Navigation,
  ],
  providers: [
    provideMonacoEditor({baseUrl: 'assets'})
  ]
}
