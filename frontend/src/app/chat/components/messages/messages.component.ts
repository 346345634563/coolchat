import {Component, effect, ElementRef, ViewChild} from '@angular/core';
import {MessagesService} from "../../services/messages.service";
import {DatePipe} from "@angular/common";
import {Message} from "../../model/message.model";

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [
    DatePipe
  ],
  templateUrl: './messages.component.html',
  styleUrl: './messages.component.css'
})
export class MessagesComponent{
  @ViewChild('messagesContainer') private chatContainer!: ElementRef;

  // On recupere les nouveaux messages
  messages: Message[] = [];

  constructor(
      private messagesService: MessagesService,
  ) {
    effect(() => {
      const messagesSignal = this.messagesService.getMessages();
      this.messages = messagesSignal();
      this.scrollToBottom();
    });
  }

  /** Afficher la date seulement si la date du message précédent est différente du message courant. */
  showDateHeader(messages: Message[] | null, i: number) {
    if (messages != null) {
      if (i === 0) {
        return true;
      } else {
        const prev = new Date(messages[i - 1].timestamp).setHours(0, 0, 0, 0);
        const curr = new Date(messages[i].timestamp).setHours(0, 0, 0, 0);
        return prev != curr;
      }
    }
    return false;
  }

  scrollToBottom(): void {
    try {
      // timeout to allow DOM to render
      setTimeout(() =>
          this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight, 0);
    } catch (err) {
      console.error('Scroll Error: ', err);
    }
  }
}
