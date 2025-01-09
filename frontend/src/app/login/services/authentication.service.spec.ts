import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthenticationService } from './authentication.service';
import { environment } from 'src/environments/environment';
import { provideHttpClient } from '@angular/common/http';

describe('AuthenticationService', () => {
  let service: AuthenticationService;
  let httpTestingController: HttpTestingController;

  const loginData = {
    username: 'username',
    password: 'pwd',
  };

  afterEach(() => {
    localStorage.clear();
  });

  describe('on login', () => {
    beforeEach(() => {
      localStorage.clear();
      TestBed.configureTestingModule({
        providers: [provideHttpClient(), provideHttpClientTesting()],
      });
      httpTestingController = TestBed.inject(HttpTestingController);
      service = TestBed.inject(AuthenticationService);
    });

    it('should call POST with login data to auth/login', async () => {
      const loginPromise = service.login(loginData);

      const req = httpTestingController.expectOne(
          `${environment.backendUrl}/auth/login`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(loginData);
      req.flush({ username: loginData.username });

      // wait for the login to complete
      await loginPromise;
    });

    it('should store and emit the username', async () => {
      // Perform login
      const loginPromise = service.login(loginData);

      // Create the expected request and flush with response
      const req = httpTestingController.expectOne(
          `${environment.backendUrl}/auth/login`
      );
      req.flush({ username: loginData.username });

      // Wait for login to complete
      await loginPromise;

      // Check that username is stored in localStorage
      expect(localStorage.getItem('username')).toBe(loginData.username);

      // Check that username signal is set
      expect(service.getUsername()()).toBe(loginData.username);

      // Check that isConnected returns true
      expect(service.isConnected()).toBe(true);
    });
  });

  describe('on logout', () => {
    beforeEach(() => {
      localStorage.setItem('username', loginData.username);

      TestBed.configureTestingModule({
        providers: [provideHttpClient(), provideHttpClientTesting()],
      });
      httpTestingController = TestBed.inject(HttpTestingController);
      service = TestBed.inject(AuthenticationService);
    });

    it('should call POST with login data to auth/logout', async () => {
      // Set up the logout promise
      const logoutPromise = service.logout();

      // Expect a POST request to the logout endpoint
      const req = httpTestingController.expectOne(
          `${environment.backendUrl}/auth/logout`
      );

      // Verify it's a POST request
      expect(req.request.method).toBe('POST');

      // Flush the request
      req.flush({});

      // Wait for logout to complete
      await logoutPromise;
    });

    it('should remove the username from the service and local storage', async () => {
      // Perform logout
      const logoutPromise = service.logout();

      // Create and flush the logout request
      const req = httpTestingController.expectOne(
          `${environment.backendUrl}/auth/logout`
      );
      req.flush({});

      // Wait for logout to complete
      await logoutPromise;

      // Check that username is removed from localStorage
      expect(localStorage.getItem('username')).toBeNull();

      // Check that username signal is set to null
      expect(service.getUsername()()).toBeNull();

      // Check that isConnected returns false
      expect(service.isConnected()).toBe(false);
    });
  });
});