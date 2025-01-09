import {CanActivateFn, Router} from '@angular/router';
import {AuthenticationService} from "../login/services/authentication.service";
import {inject} from "@angular/core";

export const loginPageGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthenticationService);
  const router = inject(Router);

  if(authService.isConnected())
    return router.parseUrl("/chat");

  return true;
};
