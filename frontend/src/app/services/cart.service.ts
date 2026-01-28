import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environments';
import { AuthService } from './auth.service';
import { Cart, CartItem } from '../models/cart.model';

interface CartDto {
  id?: string;
  userId: string;
  items: CartItemDto[];
  createdAt?: string;
  updatedAt?: string;
  total?: number;
}

interface CartItemDto {
  productId: string;
  productName?: string;
  quantity: number;
  price: number;
  available?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly apiUrl = `${environment.apiUrl}/cart`;
  private cartSubject = new BehaviorSubject<Cart>({
    userId: '',
    items: [],
    total: 0,
  });

  public cart$ = this.cartSubject.asObservable();

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService,
  ) {
    // Load cart from backend when service initializes (if user is logged in)
    if (this.authService.isLoggedIn()) {
      this.loadCart().subscribe({
        error: (err: unknown) => {
          console.error('Failed to load cart on init:', err);
        },
      });
    }
  }

  /**
   * Get cart from backend
   */
  getCart(): Cart {
    return this.cartSubject.value;
  }

  /**
   * Load cart from backend API
   */
  loadCart(): Observable<CartDto> {
    return this.http.get<CartDto>(this.apiUrl, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    }).pipe(
      tap((cartDto: CartDto) => {
        const cart = this.mapDtoToCart(cartDto);
        this.cartSubject.next(cart);
      }),
    );
  }

  /**
   * Add item to cart
   */
  addToCart(item: CartItem): Observable<CartDto> {
    const itemDto: CartItemDto = {
      productId: item.productId,
      productName: item.productName,
      quantity: item.quantity,
      price: item.price,
    };

    return this.http.post<CartDto>(`${this.apiUrl}/items`, itemDto, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    }).pipe(
      tap((cartDto: CartDto) => {
        const cart = this.mapDtoToCart(cartDto);
        this.cartSubject.next(cart);
      }),
    );
  }

  /**
   * Remove item from cart
   */
  removeFromCart(productId: string): Observable<CartDto> {
    return this.http.delete<CartDto>(`${this.apiUrl}/items/${productId}`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    }).pipe(
      tap((cartDto: CartDto) => {
        const cart = this.mapDtoToCart(cartDto);
        this.cartSubject.next(cart);
      }),
    );
  }

  /**
   * Update item quantity in cart
   */
  updateQuantity(productId: string, quantity: number): Observable<CartDto> {
    if (quantity <= 0) {
      return this.removeFromCart(productId);
    }

    const params = new HttpParams().set('quantity', quantity.toString());
    return this.http.put<CartDto>(
      `${this.apiUrl}/items/${productId}`,
      null,
      {
        headers: this.authService.getAuthHeaders(),
        withCredentials: true,
        params,
      },
    ).pipe(
      tap((cartDto: CartDto) => {
        const cart = this.mapDtoToCart(cartDto);
        this.cartSubject.next(cart);
      }),
    );
  }

  /**
   * Clear entire cart
   */
  clearCart(): Observable<void> {
    return this.http.delete<void>(this.apiUrl, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    }).pipe(
      tap(() => {
        const emptyCart: Cart = {
          userId: this.cartSubject.value.userId,
          items: [],
          total: 0,
        };
        this.cartSubject.next(emptyCart);
      }),
    );
  }

  /**
   * Get total number of items in cart
   */
  getCartItemCount(): number {
    return this.cartSubject.value.items.reduce((sum: number, item: CartItem) => sum + item.quantity, 0);
  }

  /**
   * Map backend CartDto to frontend Cart model
   */
  private mapDtoToCart(cartDto: CartDto): Cart {
    return {
      userId: cartDto.userId || '',
      items: cartDto.items.map((itemDto: CartItemDto) => ({
        productId: itemDto.productId,
        productName: itemDto.productName || '',
        sellerId: '', // Backend doesn't return sellerId in CartItemDto, will need to fetch separately if needed
        price: itemDto.price,
        quantity: itemDto.quantity,
        stock: 0, // Backend doesn't return stock in CartItemDto
        available: itemDto.available ?? true,
      })),
      total: cartDto.total || 0,
    };
  }
}
