import {
  Component,
  Input,
  Output,
  EventEmitter,
  TemplateRef,
  importProvidersFrom,
  inject, HostListener, HostBinding, Pipe, ChangeDetectionStrategy
} from '@angular/core';
import {GemFileInputComponent, config} from
  '../../../../../../build/gui2java2ts/mc/fenix/input/GemFileInputComponent';

@Component({
  standalone: true,
  imports: config.imports,
  selector: config.selector,
  templateUrl: "./GemFileInput.html",
  styles: config.styles,
  // styleUrls: [ "./GemFileInput.scss"]
  styleUrls: config.styleUrls,
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class GemFileInput extends GemFileInputComponent {


  public isCurrentlyUploading: boolean = false;
  public errorMessage?: string = null;


  @HostBinding('class.drop-zone-active')
  activeDropZone = false;


  onUploadEvt($event) {
    this.isCurrentlyUploading = true;
    this.uploadFile($event.target.files[0])
  }

  @HostListener('dragover', ['$event'])
  public onDragOver(event: DragEvent) {
    event.preventDefault()
    event.stopPropagation();
    this.activeDropZone = true;
  }


  @HostListener('dragleave', ['$event'])
  onDragLeave(event: DragEvent) {
    this.activeDropZone = false;
  }


  @HostListener('drop', ['$event'])
  public onDropFile(event: DragEvent) {
    event.preventDefault()
    this.activeDropZone = false;
    console.warn(event);

    if (event.dataTransfer.items) {
      if (event.dataTransfer.items.length != 1) {
        this.errorMessage = "Only supporting single files";
        console.warn(event.dataTransfer.items.length);
        return;
      }
      if (event.dataTransfer.items[0].kind !== 'file') {
        this.errorMessage = "Only supporting files";
        console.warn(event.dataTransfer.items[0].kind);
        return;
      }
      console.warn(event.dataTransfer.items[0]);

      this.uploadFile(event.dataTransfer.items[0].getAsFile());
    }
  }

  protected uploadFile(file: File) {
    this.errorMessage = null;
    this.isCurrentlyUploading = true;

    const data = new FormData()
    data.append('file', file)

    fetch(this.getTargetURL(), {
      method: 'PUT',
      body: data
    }).then(resp => resp.json())
      .then(resp => {
        console.warn(resp);
        // TODO: Add some feedback here?
      })
      .catch(error => {
        this.errorMessage = 'Upload failed!';
        console.error(error);
      }).finally(() => {
      this.isCurrentlyUploading = false;

    });

  }

}

