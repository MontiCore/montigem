/* (c) https://github.com/MontiCore/monticore */
import {NoteComponent,config} from "gui/note/NoteComponent";
import {Component,ChangeDetectionStrategy} from '@angular/core';
import {SehubManager} from 'sehub/SehubManager';
import {Post} from 'sehub/Post';
import {SehubSecurityManager} from "sehub/SehubSecurityManager";
import {AsyncSet, AsyncList, Gem} from '@umlp/commonj2ts';

import { CommonModule } from "@angular/common";
import { GemCard, GemColumn, GemRow } from "@umlp/arrange";
import { GemButton, GemText } from "@umlp/basic";
import { GemTextareaInput, GemTextInput } from "@umlp/input";
import { Response } from '../../../gui2java2ts/gui/note/Response';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:
[
  CommonModule,
  GemCard,
  GemColumn,
  GemButton,
  GemTextareaInput,
  GemText,
  GemTextInput,
  Response,
  GemRow,
]
,
  selector: config.selector,
  templateUrl: config.templateUrl,
  styles: config.styles,
  styleUrls: config.styleUrls
})
export class Note extends NoteComponent {

  public showResponsesFlag: boolean

  public  getShowResponses (): boolean {
    return this.showResponsesFlag;
  }

  public  getReplybuttonLabel (): string {
    return "↪️ Reply (" + this.getPost().getResponsesList().size() + ")";
  }

  override answerPost(): (arg: void) => void {
    return $ => {
      SehubManager.postBuilder().then(builder => {
        let date = new Date();

        if(!SehubSecurityManager.getCurrentUserIdOrNull()){
          alert("No User Logged In")
        } else {
          const l : AsyncSet<Post> = new AsyncSet();
          l._data = [];
          SehubSecurityManager.getCurrentUser().then(u => {
            builder.responses(l)
                .then(builder => builder.title(" | " + date.getHours() + ":" + (date.getMinutes()<10?'0':'') + date.getMinutes() + " ]: " + this.post.getTitle()))
                .then(builder => builder.author(u))
                .then(builder => builder.text("<< YOUR REPLY >>"))
                .then(builder => builder.parent(this.getPost()))
                .then(builder => builder.build())
                .then(builder => this.post.addResponses$sehub_Post(builder))
          })
        }
      })
      console.log("Responding")
    }
  }

  override deletePost(): (arg: void) => void {
    return $ => {
      this.getPost().getIssueGem().isPresent().then(b => {
        if(b){
          this.getPost().getIssue().then(i => i.removePosts$sehub_Post(this.getPost()));
        }
        SehubManager.deletePost(this.getPost().getGemId());
      })

      console.log("Removing... " + this.post.getResponsesList())
    }
  }

}
