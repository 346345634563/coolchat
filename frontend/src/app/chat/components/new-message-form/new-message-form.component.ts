import {Component, output} from '@angular/core';
import {FormBuilder, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatIconButton} from "@angular/material/button";
import {MatIcon, MatIconModule} from "@angular/material/icon";
import {MatFormField, MatSuffix} from "@angular/material/form-field";
import {MatInput} from "@angular/material/input";
import {FileReaderService} from "../../services/file-reader-service.service";
import {ChatImageData} from "../../model/message.model";

@Component({
  selector: 'app-new-message-form',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatIconButton,
    MatIcon,
    MatIconModule,
    MatFormField,
    MatInput,
    MatSuffix
  ],
  templateUrl: './new-message-form.component.html',
  styleUrl: './new-message-form.component.css'
})
export class NewMessageFormComponent {
    file: File | null = null;

    publishMessage = output<{ message: string; imageData: ChatImageData | null }>();


    messageForm = this.fb.group({
        msg: '',
    });

    constructor(
        private fb: FormBuilder,
        private fileReaderService: FileReaderService,
    ) {}

    get hasImage() {
      return this.file != null;
    }

    fileChanged(event: Event) {
        const input = event.target as HTMLInputElement;
        this.file = input.files ? input.files[0] : null;
    }

    async onPublishMessage() {
        let imageData: ChatImageData | null = null;
        if(this.file){
            imageData = await this.fileReaderService.readFile(this.file);
        }
        if (
            this.messageForm.valid &&
            this.messageForm.value.msg || this.file
        ) {
            this.publishMessage.emit({message: this.messageForm.value.msg ?? "", imageData:  imageData});
        }
        this.messageForm.reset();
        this.file = null;
    }
}
