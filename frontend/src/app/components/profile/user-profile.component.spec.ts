import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { of, throwError, BehaviorSubject } from 'rxjs';
import { UserProfileComponent } from './user-profile.component';
import { AuthService } from '../../services/auth.service';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { MediaService } from '../../services/media.service';
import { Cart, CartItem } from '../../models/cart.model';
import { Avatar, Media, Product, User } from '../../models/ecommerce.model';
import { Component } from '@angular/core';

// Mock ImageSliderComponent to avoid template issues
@Component({
  selector: 'app-image-slider',
  template: '',
  standalone: true,
})
class MockImageSliderComponent {}

describe('UserProfileComponent', () => {
  let component: UserProfileComponent;
  let fixture: ComponentFixture<UserProfileComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let mediaServiceSpy: jasmine.SpyObj<MediaService>;
  let router: Router;

  const mockUser: User = {
    id: '1',
    name: 'Test User',
    email: 'test@example.com',
    role: 'seller',
    avatar: 'avatar-123',
  };

  const mockBuyerUser: User = {
    id: '2',
    name: 'Buyer User',
    email: 'buyer@example.com',
    role: 'client',
  };

  const mockProducts: Product[] = [
    { id: 'p1', name: 'Product 1', description: 'Desc 1', price: 10, stock: 5, user: 'test@example.com' },
    { id: 'p2', name: 'Product 2', description: 'Desc 2', price: 20, stock: 10, user: 'other@example.com' },
  ];

  const mockCartItem: CartItem = {
    productId: 'p1',
    productName: 'Product 1',
    sellerId: 'seller1',
    quantity: 2,
    price: 10,
    stock: 5,
  };

  const mockCart: Cart = {
    userId: '1',
    items: [mockCartItem],
    total: 20,
  };

  const mockCartDto = {
    id: 'cart-1',
    userId: '1',
    items: [{ productId: 'p1', productName: 'Product 1', quantity: 2, price: 10 }],
    total: 20,
  };

  let cartSubject: BehaviorSubject<Cart>;

  function setupTestBed(user: User | null, isSeller: boolean = false) {
    cartSubject = new BehaviorSubject<Cart>(mockCart);

    authServiceSpy = jasmine.createSpyObj('AuthService', [
      'getCurrentUser',
      'isSeller',
      'isClient',
      'updateCurrentUser',
    ]);
    productServiceSpy = jasmine.createSpyObj('ProductService', ['getAllProducts']);
    cartServiceSpy = jasmine.createSpyObj('CartService', ['removeFromCart', 'updateQuantity'], {
      cart$: cartSubject.asObservable(),
    });
    mediaServiceSpy = jasmine.createSpyObj('MediaService', [
      'getAvatarFileUrl',
      'getMediaByProduct',
      'getMediaFile',
      'uploadAvatar',
      'deleteAvatar',
    ]);

    authServiceSpy.getCurrentUser.and.returnValue(user);
    authServiceSpy.isSeller.and.returnValue(isSeller);
    authServiceSpy.isClient.and.returnValue(!isSeller);
    productServiceSpy.getAllProducts.and.returnValue(of(mockProducts));
    mediaServiceSpy.getAvatarFileUrl.and.returnValue('http://avatar-url.com/avatar');
    mediaServiceSpy.getMediaByProduct.and.returnValue(of([]));
    mediaServiceSpy.getMediaFile.and.returnValue('http://media-url.com/file');

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, MockImageSliderComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ProductService, useValue: productServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
      ],
    }).overrideComponent(UserProfileComponent, {
      remove: { imports: [] },
      add: { imports: [MockImageSliderComponent] },
    });

    fixture = TestBed.createComponent(UserProfileComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  }

  describe('when user is not logged in', () => {
    beforeEach(() => {
      setupTestBed(null, false);
    });

    it('should redirect to login if no user is logged in', () => {
      fixture.detectChanges();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('when user is a seller', () => {
    beforeEach(() => {
      setupTestBed(mockUser, true);
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should load user data on init', () => {
      expect(component.currentUser).toEqual(mockUser);
    });

    it('should subscribe to cart updates', () => {
      expect(component.cart).toEqual(mockCart);
    });

    it('should load seller products filtered by current user email', () => {
      expect(productServiceSpy.getAllProducts).toHaveBeenCalled();
      expect(component.sellerProducts.length).toBe(1);
      expect(component.sellerProducts[0].id).toBe('p1');
    });

    it('should load avatar for seller with existing avatar', () => {
      expect(mediaServiceSpy.getAvatarFileUrl).toHaveBeenCalledWith('avatar-123');
      expect(component.avatarUrl).toBe('http://avatar-url.com/avatar');
    });

    it('should return true for isSeller()', () => {
      expect(component.isSeller()).toBe(true);
    });

    it('should return false for isBuyer()', () => {
      expect(component.isBuyer()).toBe(false);
    });

    it('should return user initials', () => {
      expect(component.getInitials()).toBe('TU');
    });

    it('should return empty array for getProductImages when no images exist', () => {
      expect(component.getProductImages('nonexistent')).toEqual([]);
    });
  });

  describe('when user is a buyer', () => {
    beforeEach(() => {
      setupTestBed(mockBuyerUser, false);
      fixture.detectChanges();
    });

    it('should not load seller products for buyer', () => {
      expect(productServiceSpy.getAllProducts).not.toHaveBeenCalled();
    });

    it('should return false for isSeller()', () => {
      expect(component.isSeller()).toBe(false);
    });

    it('should return true for isBuyer()', () => {
      expect(component.isBuyer()).toBe(true);
    });
  });

  describe('cart operations', () => {
    beforeEach(() => {
      setupTestBed(mockUser, true);
      fixture.detectChanges();
    });

    it('should remove item from cart', () => {
      cartServiceSpy.removeFromCart.and.returnValue(of(mockCartDto as any));
      component.removeFromCart('p1');
      expect(cartServiceSpy.removeFromCart).toHaveBeenCalledWith('p1');
    });

    it('should handle error when removing from cart', () => {
      const consoleSpy = spyOn(console, 'error');
      cartServiceSpy.removeFromCart.and.returnValue(throwError(() => new Error('Failed')));
      component.removeFromCart('p1');
      expect(consoleSpy).toHaveBeenCalled();
    });

    it('should update quantity when value is positive', () => {
      cartServiceSpy.updateQuantity.and.returnValue(of(mockCartDto as any));
      const mockEvent = { target: { value: '5' } } as unknown as Event;
      component.updateQuantity('p1', mockEvent);
      expect(cartServiceSpy.updateQuantity).toHaveBeenCalledWith('p1', 5);
    });

    it('should not update quantity if value is 0 or less', () => {
      const mockEvent = { target: { value: '0' } } as unknown as Event;
      component.updateQuantity('p1', mockEvent);
      expect(cartServiceSpy.updateQuantity).not.toHaveBeenCalled();
    });

    it('should handle error when updating quantity', () => {
      const consoleSpy = spyOn(console, 'error');
      cartServiceSpy.updateQuantity.and.returnValue(throwError(() => new Error('Failed')));
      const mockEvent = { target: { value: '3' } } as unknown as Event;
      component.updateQuantity('p1', mockEvent);
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe('avatar management', () => {
    beforeEach(() => {
      setupTestBed(mockUser, true);
      fixture.detectChanges();
    });

    it('should reject invalid file type', () => {
      const mockFile = new File([''], 'test.pdf', { type: 'application/pdf' });
      const mockInput = { files: [mockFile], value: '' } as unknown as HTMLInputElement;
      const mockEvent = { target: mockInput } as unknown as Event;
      component.onAvatarFileSelected(mockEvent);
      expect(component.avatarError).toContain('Invalid file type');
    });

    it('should reject file exceeding size limit', () => {
      const largeContent = new Array(3 * 1024 * 1024).fill('a').join('');
      const mockFile = new File([largeContent], 'test.jpg', { type: 'image/jpeg' });
      const mockInput = { files: [mockFile], value: '' } as unknown as HTMLInputElement;
      const mockEvent = { target: mockInput } as unknown as Event;
      component.onAvatarFileSelected(mockEvent);
      expect(component.avatarError).toContain('2MB');
    });

    it('should do nothing if no files selected (null)', () => {
      const mockInput = { files: null } as unknown as HTMLInputElement;
      const mockEvent = { target: mockInput } as unknown as Event;
      component.onAvatarFileSelected(mockEvent);
      expect(component.avatarError).toBe('');
    });

    it('should do nothing if files array is empty', () => {
      const mockInput = { files: [] } as unknown as HTMLInputElement;
      const mockEvent = { target: mockInput } as unknown as Event;
      component.onAvatarFileSelected(mockEvent);
      expect(component.avatarError).toBe('');
    });

    it('should upload valid avatar file', () => {
      const mockAvatar: Avatar = {
        id: 'new-avatar-id',
        imagePath: '/avatars/new-avatar-id',
        userId: '1',
        fileName: 'test.jpg',
        contentType: 'image/jpeg',
        fileSize: 1024,
      };
      mediaServiceSpy.uploadAvatar.and.returnValue(of(mockAvatar));
      const mockFile = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
      const mockInput = { files: [mockFile], value: '' } as unknown as HTMLInputElement;
      const mockEvent = { target: mockInput } as unknown as Event;
      component.onAvatarFileSelected(mockEvent);
      expect(mediaServiceSpy.uploadAvatar).toHaveBeenCalledWith(mockFile);
      expect(authServiceSpy.updateCurrentUser).toHaveBeenCalledWith({ avatar: 'new-avatar-id' });
    });

    it('should handle avatar upload error with message', () => {
      const consoleSpy = spyOn(console, 'error');
      mediaServiceSpy.uploadAvatar.and.returnValue(throwError(() => ({ error: { message: 'Upload failed' } })));
      const mockFile = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
      component.uploadAvatar(mockFile);
      expect(consoleSpy).toHaveBeenCalled();
      expect(component.avatarError).toBe('Upload failed');
      expect(component.isUploadingAvatar).toBe(false);
    });

    it('should handle avatar upload error without message', () => {
      spyOn(console, 'error');
      mediaServiceSpy.uploadAvatar.and.returnValue(throwError(() => ({})));
      const mockFile = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
      component.uploadAvatar(mockFile);
      expect(component.avatarError).toBe('Failed to upload avatar');
    });

    it('should delete avatar when confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      mediaServiceSpy.deleteAvatar.and.returnValue(of(void 0 as any));
      component.deleteAvatar();
      expect(mediaServiceSpy.deleteAvatar).toHaveBeenCalled();
      expect(component.avatarUrl).toBeNull();
      expect(authServiceSpy.updateCurrentUser).toHaveBeenCalledWith({ avatar: undefined });
    });

    it('should not delete avatar when not confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.deleteAvatar();
      expect(mediaServiceSpy.deleteAvatar).not.toHaveBeenCalled();
    });

    it('should handle avatar delete error with message', () => {
      const consoleSpy = spyOn(console, 'error');
      spyOn(window, 'confirm').and.returnValue(true);
      mediaServiceSpy.deleteAvatar.and.returnValue(throwError(() => ({ error: { message: 'Delete failed' } })));
      component.deleteAvatar();
      expect(consoleSpy).toHaveBeenCalled();
      expect(component.avatarError).toBe('Delete failed');
      expect(component.isUploadingAvatar).toBe(false);
    });

    it('should handle avatar delete error without message', () => {
      spyOn(console, 'error');
      spyOn(window, 'confirm').and.returnValue(true);
      mediaServiceSpy.deleteAvatar.and.returnValue(throwError(() => ({})));
      component.deleteAvatar();
      expect(component.avatarError).toBe('Failed to delete avatar');
    });
  });

  describe('product images', () => {
    it('should load product images when media exists', () => {
      const mockMedia: Media[] = [
        { id: 'media1', imagePath: '/media/media1', productId: 'p1', fileName: 'img1.jpg', contentType: 'image/jpeg', fileSize: 1024 },
        { id: 'media2', imagePath: '/media/media2', productId: 'p1', fileName: 'img2.jpg', contentType: 'image/jpeg', fileSize: 2048 },
      ];
      setupTestBed(mockUser, true);
      mediaServiceSpy.getMediaByProduct.and.returnValue(of(mockMedia));
      mediaServiceSpy.getMediaFile.and.callFake((id: string) => `http://media-url.com/${id}`);
      fixture.detectChanges();
      expect(component.getProductImages('p1')).toEqual(['http://media-url.com/media1', 'http://media-url.com/media2']);
    });

    it('should handle error loading product images silently', () => {
      setupTestBed(mockUser, true);
      mediaServiceSpy.getMediaByProduct.and.returnValue(throwError(() => new Error('Failed')));
      fixture.detectChanges();
      expect(component.getProductImages('p1')).toEqual([]);
    });
  });

  describe('edge cases', () => {
    it('should return "U" for getInitials when user has no name', () => {
      const userNoName: User = { ...mockUser, name: '' };
      setupTestBed(userNoName, true);
      fixture.detectChanges();
      expect(component.getInitials()).toBe('U');
    });

    it('should handle error when loading seller products', () => {
      const consoleSpy = spyOn(console, 'error');
      setupTestBed(mockUser, true);
      productServiceSpy.getAllProducts.and.returnValue(throwError(() => new Error('Failed')));
      fixture.detectChanges();
      expect(consoleSpy).toHaveBeenCalled();
      expect(component.isLoading).toBe(false);
    });

    it('should not load avatar if user has no avatar', () => {
      const userNoAvatar: User = { ...mockUser, avatar: undefined };
      setupTestBed(userNoAvatar, true);
      mediaServiceSpy.getAvatarFileUrl.calls.reset();
      fixture.detectChanges();
      expect(mediaServiceSpy.getAvatarFileUrl).not.toHaveBeenCalled();
    });
  });
});
