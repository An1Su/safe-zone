import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Order } from '../../models/order.model';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-order-confirmation',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-confirmation.component.html',
  styleUrl: './order-confirmation.component.scss',
})
export class OrderConfirmationComponent implements OnInit {
  order: Order | null = null;
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (orderId) {
      this.loadOrder(orderId);
    } else {
      this.error = 'Order not found';
      this.loading = false;
    }
  }

  loadOrder(orderId: string): void {
    this.orderService.getOrderById(orderId).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load order details';
        this.loading = false;
      },
    });
  }

  getStatusClass(status: string): string {
    const statusClasses: Record<string, string> = {
      PENDING: 'status-pending',
      READY_FOR_DELIVERY: 'status-ready-for-delivery',
      SHIPPED: 'status-shipped',
      DELIVERED: 'status-delivered',
      CANCELLED: 'status-cancelled',
    };
    return statusClasses[status] || 'status-pending';
  }

  formatDate(date: Date | string | undefined): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}

