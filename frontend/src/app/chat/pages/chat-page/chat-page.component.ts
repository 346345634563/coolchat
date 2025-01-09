import { Component } from '@angular/core';
import { AuthenticationService } from 'src/app/login/services/authentication.service';
import {MessagesComponent} from "../../components/messages/messages.component";
import {NewMessageFormComponent} from "../../components/new-message-form/new-message-form.component";
import {MatButton} from "@angular/material/button";
import {WebSocketEvent, WebSocketService} from "../../services/websocket.service";
import {MessagesService} from "../../services/messages.service";
import {Router} from "@angular/router";
import {ChatImageData} from "../../model/message.model";
import {HttpErrorResponse} from "@angular/common/http";

@Component({
  selector: 'app-chat-page',
  templateUrl: './chat-page.component.html',
  styleUrls: ['./chat-page.component.css'],
  standalone: true,
  imports: [MessagesComponent, NewMessageFormComponent, MatButton],
})

export class ChatPageComponent {
  username = this.authenticationService.getUsername();

  constructor(
    private authenticationService: AuthenticationService,
    private wsService: WebSocketService,
    private messagesService: MessagesService,
    private router: Router,
  ) {}

  ngOnInit(){
    //this.fetchMessages();
    this.wsService.initializeWebSocket();
    this.wsService.getConnectionStatus().subscribe(isConnected => {
      if(isConnected) {
        this.fetchMessages();
      }
    })
    this.wsService.connect().subscribe({
      next: async (event: WebSocketEvent) => {
        if (event === 'notif') {
          await this.fetchMessages();
        }
      },
      error: (err) => console.error('WebSocket error', err),
      complete: () => console.log('WebSocket connection closed'),
    });
  }

  private async fetchMessages(){
    try{
      await this.messagesService.fetchMessages();
    } catch (e){
      if (e instanceof HttpErrorResponse && e.status === 403) {
        await this.onLogout();
      }
    }
  }

  ngOnDestroy(){
    this.wsService.disconnect();
  }

  async onPublishMessage(event: { message: string; imageData: ChatImageData | null }) {
    try{
      await this.messagesService.postMessage({
        text: event.message,
        username: this.username() ?? "",
        imageData: event.imageData,
      });
    } catch (e){
      if (e instanceof HttpErrorResponse && e.status === 403) {
        await this.onLogout();
      }
    }
  }

  async onLogout() {
    // disconnect user and navigate back to root
    await this.authenticationService.logout();
    await this.router.navigate(['/']);
  }
}
