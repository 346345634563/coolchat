import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { LoginFormComponent } from './login-form.component';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {TestHelper} from '../../../tests/TestHelper';

describe('LoginFormComponent', () => {
  let component: LoginFormComponent;
  let fixture: ComponentFixture<LoginFormComponent>;
  let testHelper: TestHelper<LoginFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, LoginFormComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginFormComponent);
    testHelper = new TestHelper(fixture);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit username and password', () => {
    let username: string;
    let password: string;

    // On s'abonne à l'EventEmitter pour recevoir les valeurs émises.
    component.login.subscribe((event) => {
      username = event.username;
      password = event.password;
    });
    
    // on rempli le formulaire
    const userInput = testHelper.getInput('user-input');
    testHelper.writeInInput(userInput, 'username')
    const passwordInput = testHelper.getInput('password-input');
    testHelper.writeInInput(passwordInput, 'pwd')

    // On simule un clique sur le bouton.
    const button = testHelper.getButton('connect-button');
    button.click();

    expect(username!).toBe('username');
    expect(password!).toBe('pwd');
  });

  it('should display error when no username', () => {

    // On s'abonne à l'EventEmitter pour recevoir les valeurs émises.
    component.login.subscribe((event) => {
      fail('No event should be emitted when login fails');
    });

    // on rempli le mot de passe
    const passwordInput = testHelper.getInput('password-input');
    testHelper.writeInInput(passwordInput, 'pwd')

    // On simule un clique sur le bouton
    const button = testHelper.getButton('connect-button');
    button.click();

    // On force la détection de changement.
    fixture.detectChanges();

    const errorMessage = testHelper.getElement('error-message');
    expect(errorMessage.innerText).toBe('Entrer un utilisateur')
  });

  it('should display error when no password', () => {

    // On s'abonne à l'EventEmitter pour recevoir les valeurs émises.
    component.login.subscribe((event) => {
      fail('No event should be emitted when login fails');
    });

    // on rempli le mot de passe
    const userInput = testHelper.getInput('user-input');
    testHelper.writeInInput(userInput, 'pwd')

    // On simule un clique sur le bouton
    const button = testHelper.getButton('connect-button');
    button.click();

    // On force la détection de changement.
    fixture.detectChanges();

    const errorMessage = testHelper.getElement('error-message');
    expect(errorMessage.innerText).toBe('Entrer un mot de passe')
  });

  it('should display error when no password and no user', () => {

    // On s'abonne à l'EventEmitter pour recevoir les valeurs émises.
    component.login.subscribe((event) => {
      fail('No event should be emitted when login fails');
    });

    // On simule un clique sur le bouton
    const button = testHelper.getButton('connect-button');
    button.click();

    // On force la détection de changement.
    fixture.detectChanges();

    const errorMessage = testHelper.getElement('error-message');
    expect(errorMessage.innerText).toBe('Entrer un utilisateur et un mot de passe')
  });
});
