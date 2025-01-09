import { Component } from '@angular/core';
import { LoginFormComponent } from '../../components/login-form/login-form.component';
import { UserCredentials } from '../../model/user-credentials';
import { AuthenticationService } from '../../services/authentication.service';
import { Router } from "@angular/router";
import {HttpErrorResponse} from "@angular/common/http";

@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css'],
  standalone: true,
  imports: [LoginFormComponent],
})

export class LoginPageComponent {
  errorMessage = "";

  constructor(private authService: AuthenticationService, private router: Router) {}

  async onLogin(userCredentials: UserCredentials) {

    try {
      await this.authService.login(userCredentials);
      await this.router.navigate(['/chat']);
    } catch (e){
      if (e instanceof HttpErrorResponse && e.status === 403) {
        this.errorMessage = "Mot de passe invalide";
      } else {
        this.errorMessage = "Problème de connexion";
      }
    }
  }
}
