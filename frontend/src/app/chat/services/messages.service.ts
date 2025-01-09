import { Injectable, Signal, signal } from '@angular/core';
import {Message, NewMessageRequest} from '../model/message.model';
import { HttpClient } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import {firstValueFrom} from "rxjs";

@Injectable({
  providedIn: 'root',
})
export class MessagesService {
  messages = signal<Message[]>([]);
  lastMessageId = "";

  constructor(private httpClient: HttpClient) {
  }

  async postMessage(message: NewMessageRequest) {
    await firstValueFrom(this.httpClient
      .post<Message>(`${environment.backendUrl}/messages`, message, 
        {withCredentials: true})
    );
  }

  getMessages(): Signal<Message[]> {
    return this.messages;
  }

  async fetchMessages(){
    const receivedMessages = await firstValueFrom(this.httpClient
      .get<Message[]>(`${environment.backendUrl}/messages?fromId=${this.lastMessageId}`,
        {withCredentials: true}
      ));
    if(receivedMessages.length > 0){
      this.messages.update((messages) => {
        // remove duplicated based on ID
        const allMessages = [...messages, ...receivedMessages];
        const newMessages: Message[] = [];
        for(const message of allMessages){
          if(!newMessages.some((x) => x.id === message.id))
            newMessages.push(message);
        }
        // sort messages based on timestamp
        newMessages.sort((a, b) => a.timestamp - b.timestamp);

        // On recupere le id du dernier message
        if(newMessages.length > 0){
          this.lastMessageId = String(newMessages[newMessages.length - 1].id);
        }

        return newMessages;
      });
    }
  }
}
