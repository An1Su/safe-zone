import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Cart, CartItem } from '../models/cart.model';
import { AuthService } from './auth.service';
import { CartService } from './cart.service';
import { environment } from '../../environments/environments';

describe('CartService', () => {
  let service: CartService;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;

  const mockCartItem1: CartItem = {
    productId: '1',
    productName: 'Product 1',
    price: 99.99,
    quantity: 2,
    sellerId: 'seller-1',
    stock: 10,
    available: true,
  };

  const mockCartItem2: CartItem = {
    productId: '2',
    productName: 'Product 2',
    price: 149.99,
    quantity: 1,
    sellerId: 'seller-2',
    stock: 5,
    available: true,
  };

  const mockCartDto = {
    id: 'cart-1',
    userId: 'user-1',
    items: [
      {
        productId: '1',
        productName: 'Product 1',
        quantity: 2,
        price: 99.99,
        available: true,
      },
    ],
    total: 199.98,
    createdAt: '2026-01-28T10:00:00Z',
    updatedAt: '2026-01-28T10:00:00Z',
  };

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getAuthHeaders', 'isLoggedIn']);
    authServiceSpy.getAuthHeaders.and.returnValue(
      new HttpHeaders({ Authorization: 'Bearer mock-token' }),
    );
    // Return false to prevent auto-loading cart in constructor
    authServiceSpy.isLoggedIn.and.returnValue(false);

    TestBed.configureTestingModule({
      providers: [
        CartService,
        { provide: AuthService, useValue: authServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(CartService);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getCart', () => {
    it('should return empty cart initially', () => {
      const cart = service.getCart();
      expect(cart.items).toEqual([]);
      expect(cart.total).toBe(0);
    });
  });

  describe('loadCart', () => {
    it('should load cart from backend', () => {
      service.loadCart().subscribe((cartDto) => {
        expect(cartDto.items.length).toBe(1);
        expect(cartDto.total).toBe(199.98);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cart`);
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(authService.getAuthHeaders).toHaveBeenCalled();
      req.flush(mockCartDto);
    });

    it('should update cart$ observable when cart is loaded', (done) => {
      service.cart$.subscribe((cart) => {
        if (cart.items.length > 0) {
          expect(cart.items[0].productId).toBe('1');
          done();
        }
      });

      service.loadCart().subscribe();
      const req = httpMock.expectOne(`${environment.apiUrl}/cart`);
      req.flush(mockCartDto);
    });
  });

  describe('addToCart', () => {
    it('should add item to cart via API', () => {
      service.addToCart(mockCartItem1).subscribe((cartDto) => {
        expect(cartDto.items.length).toBe(1);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cart/items`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        productId: '1',
        productName: 'Product 1',
        quantity: 2,
        price: 99.99,
      });
      expect(req.request.withCredentials).toBe(true);
      req.flush(mockCartDto);
    });

    it('should update cart$ observable after adding item', (done) => {
      service.cart$.subscribe((cart) => {
        if (cart.items.length > 0) {
          expect(cart.items[0].productId).toBe('1');
          done();
        }
      });

      service.addToCart(mockCartItem1).subscribe();
      const req = httpMock.expectOne(`${environment.apiUrl}/cart/items`);
      req.flush(mockCartDto);
    });
  });

  describe('removeFromCart', () => {
    it('should remove item from cart via API', () => {
      const updatedCartDto = { ...mockCartDto, items: [], total: 0 };
      service.removeFromCart('1').subscribe((cartDto) => {
        expect(cartDto.items.length).toBe(0);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cart/items/1`);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.withCredentials).toBe(true);
      req.flush(updatedCartDto);
    });
  });

  describe('updateQuantity', () => {
    it('should update item quantity via API', () => {
      const updatedCartDto = {
        ...mockCartDto,
        items: [{ ...mockCartDto.items[0], quantity: 5 }],
        total: 499.95,
      };
      service.updateQuantity('1', 5).subscribe((cartDto) => {
        expect(cartDto.items[0].quantity).toBe(5);
      });

      const req = httpMock.expectOne((request) => {
        return (
          request.url === `${environment.apiUrl}/cart/items/1` &&
          request.method === 'PUT' &&
          request.params.get('quantity') === '5'
        );
      });
      req.flush(updatedCartDto);
    });

    it('should remove item if quantity is zero or negative', () => {
      const updatedCartDto = { ...mockCartDto, items: [], total: 0 };
      service.updateQuantity('1', 0).subscribe((cartDto) => {
        expect(cartDto.items.length).toBe(0);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/cart/items/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(updatedCartDto);
    });
  });

  describe('clearCart', () => {
    it('should clear cart via API', () => {
      service.clearCart().subscribe(() => {
        const cart = service.getCart();
        expect(cart.items.length).toBe(0);
        expect(cart.total).toBe(0);
      });

      // Only expect DELETE request, not GET
      const req = httpMock.expectOne((request) => request.method === 'DELETE' && request.url === `${environment.apiUrl}/cart`);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.withCredentials).toBe(true);
      req.flush(null);
    });
  });

  describe('getCartItemCount', () => {
    it('should return 0 for empty cart', () => {
      const count = service.getCartItemCount();
      expect(count).toBe(0);
    });

    it('should return total quantity of items', () => {
      // Set cart items directly for testing
      const cartWithItems: Cart = {
        userId: 'user-1',
        items: [mockCartItem1, mockCartItem2],
        total: 349.97,
      };
      // Access private cartSubject via any cast for testing
      (service as any).cartSubject.next(cartWithItems);

      const count = service.getCartItemCount();
      expect(count).toBe(3); // 2 + 1
    });
  });
});
