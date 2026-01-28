import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environments';
import { CreateOrderRequest, Order, OrderStats } from '../models/order.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private apiUrl = `${environment.apiUrl}/orders`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * Create a new order from cart items
   */
  createOrder(orderRequest: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, orderRequest, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  /**
   * Get current user's orders
   */
  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(this.apiUrl, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
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
      }
    );
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
}

