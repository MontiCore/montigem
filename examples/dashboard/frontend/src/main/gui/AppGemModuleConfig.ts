/* (c) https://github.com/MontiCore/monticore */
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';

export const config = {
  declarations: [],
  exports: [],
  imports: [ MonacoEditorModule.forRoot({baseUrl: 'assets'})],
  providers: []
}

