/* (c) https://github.com/MontiCore/monticore */

import { Navigation } from 'navigation/Navigation';
import { NavigationRouteSync } from 'navigation/NavigationRouteSync';
import { provideMonacoEditor } from 'ngx-monaco-editor-v2';

export const config = {
  declarations: [],
  exports: [],
  imports: [
    Navigation,
    NavigationRouteSync,
  ],
  providers: [
    provideMonacoEditor({baseUrl: 'assets'})
  ]
}
