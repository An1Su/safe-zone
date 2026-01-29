import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environments';
import {
  CreateOrderRequest,
  Order,
  OrderSearchParams,
  OrderStats,
  OrderStatus,
} from '../models/order.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private readonly apiUrl = `${environment.apiUrl}/orders`;

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService,
  ) {}

  /**
   * Create a new order from cart (backend uses cart items automatically)
   */
  createOrder(shippingAddress: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, shippingAddress, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  /**
   * Get current user's orders
   */
  getOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(this.apiUrl, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  /**
   * Alias for getOrders() - kept for backward compatibility
   */
  getMyOrders(): Observable<Order[]> {
    return this.getOrders();
  }

  /**
   * Get a specific order by ID
   */
  getOrderById(orderId: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${orderId}`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  /**
   * Cancel an order (only if PENDING or CONFIRMED)
   */
  cancelOrder(orderId: string): Observable<Order> {
    return this.http.put<Order>(
      `${this.apiUrl}/${orderId}/cancel`,
      {},
      {
        headers: this.authService.getAuthHeaders(),
        withCredentials: true,
      },
    );
  }

  /**
   * Delete an order (only if CANCELLED or DELIVERED)
   */
  deleteOrder(orderId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${orderId}`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  /**
   * Redo a cancelled order (create new order with same items)
   */
  redoOrder(orderId: string): Observable<Order> {
    return this.http.post<Order>(
      `${this.apiUrl}/${orderId}/redo`,
      {},
      {
        headers: this.authService.getAuthHeaders(),
        withCredentials: true,
      },
    );
  }

  /**
   * Search orders with filters
   */
  searchOrders(params: OrderSearchParams): Observable<Order[]> {
    let httpParams = new HttpParams();
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
    if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);

    return this.http.get<Order[]>(`${this.apiUrl}/search`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
      params: httpParams,
    });
  }

  /**
   * Get user's order statistics
   */
  getUserStats(): Observable<OrderStats> {
    return this.http.get<OrderStats>(`${this.apiUrl}/stats/user`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  // ========== Seller Endpoints ==========

  /**
   * Get seller's orders (orders containing seller's products)
   */
  getSellerOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/seller`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  /**
   * Get seller's order by ID (only seller's items)
   */
  getSellerOrderById(orderId: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/seller/${orderId}`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  /**
   * Update order status (seller only)
   */
  updateOrderStatus(orderId: string, status: OrderStatus): Observable<Order> {
    const params = new HttpParams().set('status', status);
    return this.http.put<Order>(`${this.apiUrl}/${orderId}/status`, null, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
      params,
    });
  }

  /**
   * Search seller orders with filters
   */
  searchSellerOrders(params: OrderSearchParams): Observable<Order[]> {
    let httpParams = new HttpParams();
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
    if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);

    return this.http.get<Order[]>(`${this.apiUrl}/seller/search`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
      params: httpParams,
    });
  }
}
