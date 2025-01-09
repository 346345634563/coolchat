import { Injectable, Signal, signal } from '@angular/core';
import { UserCredentials } from '../model/user-credentials';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from 'src/environments/environment';
import { HttpClient } from '@angular/common/http';
import { LoginResponse } from '../model/login-response';

@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {
  static KEY = 'username';

  private username = signal<string | null>(null);

  constructor(private router: Router, private httpClient: HttpClient) {
    this.username.set(localStorage.getItem(AuthenticationService.KEY));
  }

  // Si le login est valide on va envoyer l'utilisateur vers le chat
  async login(userCredentials: UserCredentials) {

    await firstValueFrom(
        this.httpClient.post<LoginResponse>(`${environment.backendUrl}/auth/login`,
            userCredentials, { withCredentials: true })
    );

    localStorage.setItem(AuthenticationService.KEY, userCredentials.username);
    this.username.set(userCredentials.username);
  }
  
  async logout() {
    try {
      await firstValueFrom(
        this.httpClient.post<void>(`${environment.backendUrl}/auth/logout`, {}, { withCredentials: true })
      );

      localStorage.removeItem(AuthenticationService.KEY);
      this.username.set(null);
    } catch (e) {
      console.log("Error while logging out:\n" + e);
    }
  }

  getUsername(): Signal<string | null> {
    return this.username;
  }

  isConnected(){
    return Boolean(this.username() && this.username()!.length > 0);
  }
}
