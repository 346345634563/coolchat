import {Component, EventEmitter, Input, Output, output} from '@angular/core';
import {FormBuilder, ReactiveFormsModule} from '@angular/forms';
import {UserCredentials} from '../../model/user-credentials';
import {MatFormField, MatLabel} from "@angular/material/form-field";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";

@Component({
  selector: 'app-login-form',
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.css'],
  standalone: true,
  imports: [ReactiveFormsModule, MatFormField, MatLabel, MatInputModule, MatButtonModule],
})

export class LoginFormComponent {

  loginForm = this.fb.group({
    username: '',
    password: '',
  });

  constructor(private fb: FormBuilder) {
  }

  // On declare un message pour les erreurs possibles
  @Input() errorMessage : string | null = null;

  @Output() login = new EventEmitter<UserCredentials>();



  onLogin() {
    let {username, password} = this.loginForm.value;

    // Permet de s'assurer qu'on bien un string
    username = username?.trim() ?? '';
    password = password?.trim() ?? '';

    if (!username && !password) {
      this.errorMessage = "Entrer un utilisateur et un mot de passe";
    } else if (!username) {
      this.errorMessage = "Entrer un utilisateur";
    } else if (!password) {
      this.errorMessage = "Entrer un mot de passe";
    } else{
      this.login.emit({
        username,
        password
      });
    }
  }
}
