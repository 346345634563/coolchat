import { Routes } from '@angular/router';
import { LoginPageComponent } from './login/pages/login-page/login-page.component';
import { ChatPageComponent } from './chat/pages/chat-page/chat-page.component';
import {loginPageGuard} from "./guards/login-page.guard";
import {chatPageGuard} from "./guards/chat-page.guard";



export const routes: Routes = [
    { path: '', component: LoginPageComponent, canActivate: [loginPageGuard] },
    { path: 'chat', component: ChatPageComponent, canActivate: [chatPageGuard]}
];
