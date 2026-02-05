import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of } from 'rxjs';
import { NavbarComponent } from './navbar.component';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { CartService } from '../../services/cart.service';
import { User } from '../../models/ecommerce.model';
import { Cart } from '../../models/cart.model';
import { ElementRef } from '@angular/core';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let mediaServiceSpy: jasmine.SpyObj<MediaService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let userSubject: BehaviorSubject<User | null>;
  let cartSubject: BehaviorSubject<Cart>;

  const mockUser: User = {
    id: '1',
    name: 'Test User',
    email: 'test@example.com',
    role: 'seller',
    avatar: 'avatar-123',
  };

  const mockCart: Cart = {
    userId: '1',
    items: [
      { productId: 'p1', productName: 'Product 1', sellerId: 's1', quantity: 2, price: 10, stock: 5 },
      { productId: 'p2', productName: 'Product 2', sellerId: 's1', quantity: 3, price: 20, stock: 10 },
    ],
    total: 80,
  };

  beforeEach(async () => {
    userSubject = new BehaviorSubject<User | null>(null);
    cartSubject = new BehaviorSubject<Cart>({ userId: '', items: [], total: 0 });

    authServiceSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'isSeller', 'logout'], {
      currentUser$: userSubject.asObservable(),
    });
    mediaServiceSpy = jasmine.createSpyObj('MediaService', ['getAvatarFileUrl']);
    cartServiceSpy = jasmine.createSpyObj('CartService', [], {
      cart$: cartSubject.asObservable(),
    });

    await TestBed.configureTestingModule({
      imports: [NavbarComponent, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('initialization', () => {
    it('should subscribe to currentUser$ on init', () => {
      userSubject.next(mockUser);
      mediaServiceSpy.getAvatarFileUrl.and.returnValue('http://avatar-url.com');
      fixture.detectChanges();

      expect(component.currentUser).toEqual(mockUser);
    });

    it('should load avatar for seller with avatar', () => {
      userSubject.next(mockUser);
      mediaServiceSpy.getAvatarFileUrl.and.returnValue('http://avatar-url.com');
      fixture.detectChanges();

      expect(component.avatarUrl).toBe('http://avatar-url.com');
    });

    it('should not load avatar for client user', () => {
      const clientUser: User = { ...mockUser, role: 'client' };
      userSubject.next(clientUser);
      fixture.detectChanges();

      expect(component.avatarUrl).toBeNull();
    });

    it('should not load avatar for seller without avatar', () => {
      const sellerNoAvatar: User = { ...mockUser, avatar: undefined };
      userSubject.next(sellerNoAvatar);
      fixture.detectChanges();

      expect(component.avatarUrl).toBeNull();
    });

    it('should subscribe to cart$ and calculate item count', () => {
      cartSubject.next(mockCart);
      fixture.detectChanges();

      expect(component.cartItemCount).toBe(5); // 2 + 3
    });
  });

  describe('getInitials', () => {
    it('should return initials for user with name', () => {
      component.currentUser = mockUser;
      expect(component.getInitials()).toBe('TU');
    });

    it('should return "U" for user with no name', () => {
      component.currentUser = { ...mockUser, name: '' };
      expect(component.getInitials()).toBe('U');
    });

    it('should return "U" for null user', () => {
      component.currentUser = null;
      expect(component.getInitials()).toBe('U');
    });

    it('should limit initials to 2 characters', () => {
      component.currentUser = { ...mockUser, name: 'John James Doe' };
      expect(component.getInitials()).toBe('JJ');
    });
  });

  describe('isLoggedIn', () => {
    it('should return true when user is logged in', () => {
      authServiceSpy.isLoggedIn.and.returnValue(true);
      expect(component.isLoggedIn()).toBe(true);
    });

    it('should return false when user is not logged in', () => {
      authServiceSpy.isLoggedIn.and.returnValue(false);
      expect(component.isLoggedIn()).toBe(false);
    });
  });

  describe('isSeller', () => {
    it('should return true for seller', () => {
      authServiceSpy.isSeller.and.returnValue(true);
      expect(component.isSeller()).toBe(true);
    });

    it('should return false for non-seller', () => {
      authServiceSpy.isSeller.and.returnValue(false);
      expect(component.isSeller()).toBe(false);
    });
  });

  describe('isBuyer', () => {
    it('should return true when logged in and not seller', () => {
      authServiceSpy.isLoggedIn.and.returnValue(true);
      authServiceSpy.isSeller.and.returnValue(false);
      expect(component.isBuyer()).toBe(true);
    });

    it('should return false when not logged in', () => {
      authServiceSpy.isLoggedIn.and.returnValue(false);
      authServiceSpy.isSeller.and.returnValue(false);
      expect(component.isBuyer()).toBe(false);
    });

    it('should return false when seller', () => {
      authServiceSpy.isLoggedIn.and.returnValue(true);
      authServiceSpy.isSeller.and.returnValue(true);
      expect(component.isBuyer()).toBe(false);
    });
  });

  describe('dropdown', () => {
    it('should toggle dropdown open/close', () => {
      expect(component.dropdownOpen).toBe(false);

      component.toggleDropdown();
      expect(component.dropdownOpen).toBe(true);

      component.toggleDropdown();
      expect(component.dropdownOpen).toBe(false);
    });

    it('should close dropdown', () => {
      component.dropdownOpen = true;
      component.closeDropdown();
      expect(component.dropdownOpen).toBe(false);
    });

    it('should close dropdown when clicking outside', () => {
      component.dropdownOpen = true;
      const outsideElement = document.createElement('div');
      document.body.appendChild(outsideElement);
      const event = new MouseEvent('click');
      Object.defineProperty(event, 'target', { value: outsideElement });

      component.onDocumentClick(event);

      expect(component.dropdownOpen).toBe(false);
      document.body.removeChild(outsideElement);
    });

    it('should not close dropdown when clicking inside', () => {
      component.dropdownOpen = true;
      const event = new MouseEvent('click');
      Object.defineProperty(event, 'target', { value: fixture.nativeElement });

      component.onDocumentClick(event);

      expect(component.dropdownOpen).toBe(true);
    });
  });

  describe('logout', () => {
    it('should close dropdown and call auth service logout', () => {
      authServiceSpy.logout.and.returnValue(of('Logged out'));
      component.dropdownOpen = true;

      component.logout();

      expect(component.dropdownOpen).toBe(false);
      expect(authServiceSpy.logout).toHaveBeenCalled();
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe from cart subscription', () => {
      fixture.detectChanges();
      const subscription = (component as any).cartSubscription;
      spyOn(subscription, 'unsubscribe');

      component.ngOnDestroy();

      expect(subscription.unsubscribe).toHaveBeenCalled();
    });
  });
});

