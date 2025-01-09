import { Injectable } from "@angular/core";
import { Observable, Subject, BehaviorSubject} from "rxjs";
import { environment } from "src/environments/environment";

export type WebSocketEvent = "notif";

@Injectable({
  providedIn: "root",
})
export class WebSocketService {
  private ws: WebSocket | null = null;
  private eventsSubject = new Subject<WebSocketEvent>();
  private connectionSubject = new BehaviorSubject<boolean>(false);
  private shouldReconnect = true;
  private readonly RETRY_DELAY = 2000; // 2 seconds

  public initializeWebSocket(): void {
    this.createWebSocket();
  }

  private createWebSocket(): void {
    // Ensure previous socket is closed if exists
    this.disconnect();

    this.shouldReconnect = true;

    this.ws = new WebSocket(`${environment.wsUrl}/notifications`);

    this.ws.onopen = () => {
      console.log("WebSocket connection opened");
      this.connectionSubject.next(true);
    };

    this.ws.onmessage = (event) => {
      this.eventsSubject.next("notif");
    };

    this.ws.onclose = (event) => {
      // Attempt reconnection if it should reconnect
      if (this.shouldReconnect) {
        this.reconnect();
      }
    };

    this.ws.onerror = (error) => {
      console.error("WebSocket error:", error);
    };
  }

  private reconnect(): void {
    setTimeout(() => {
      if (this.shouldReconnect) {
        console.log("Attempting to reconnect WebSocket...");
        this.createWebSocket();
      }
    }, this.RETRY_DELAY);
  }

  public connect(): Observable<WebSocketEvent> {
    return this.eventsSubject.asObservable();
  }

  public getConnectionStatus(): Observable<boolean> {
    return this.connectionSubject.asObservable();
  }

  public disconnect(): void {
    this.shouldReconnect = false;
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}