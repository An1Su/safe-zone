import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Order, OrderStatus } from '../../models/order.model';
import { MediaService } from '../../services/media.service';
import { OrderService } from '../../services/order.service';

interface OrderStats {
  totalOrders: number;
  delivered: number;
  inProgress: number;
  totalSpent: number;
}

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-history.component.html',
  styleUrl: './order-history.component.scss',
})
export class OrderHistoryComponent implements OnInit {
  orders: Order[] = [];
  loading = true;
  error = '';
  stats: OrderStats = {
    totalOrders: 0,
    delivered: 0,
    inProgress: 0,
    totalSpent: 0,
  };
  productImages: Map<string, string> = new Map();

  constructor(
    private orderService: OrderService,
    private mediaService: MediaService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.error = '';

    this.orderService.getOrders().subscribe({
      next: (orders) => {
        this.orders = orders.sort((a, b) => {
          const dateA = new Date(a.createdAt || 0).getTime();
          const dateB = new Date(b.createdAt || 0).getTime();
          return dateB - dateA;
        });
        this.calculateStats();
        this.loadProductImages();
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load orders:', err);
        this.error = 'Failed to load your order history. Please try again.';
        this.loading = false;
      },
    });
  }

  calculateStats(): void {
    this.stats = {
      totalOrders: this.orders.length,
      delivered: this.orders.filter((o) => o.status === 'DELIVERED').length,
      inProgress: this.orders.filter((o) =>
        ['PENDING', 'READY_FOR_DELIVERY', 'SHIPPED'].includes(o.status)
      ).length,
      totalSpent: this.orders
        .filter((o) => o.status !== 'CANCELLED')
        .reduce((sum, o) => sum + o.totalAmount, 0),
    };
  }

  loadProductImages(): void {
    // Collect all unique product IDs from all orders
    const productIds = new Set<string>();
    this.orders.forEach((order) => {
      order.items.forEach((item) => {
        if (!this.productImages.has(item.productId)) {
          productIds.add(item.productId);
        }
      });
    });

    // Load images for each product
    productIds.forEach((productId) => {
      this.mediaService.getMediaByProduct(productId).subscribe({
        next: (media) => {
          if (media && media.length > 0) {
            this.productImages.set(
              productId,
              this.mediaService.getMediaFile(media[0].id!)
            );
          }
        },
      });
    });
  }

  getProductImage(productId: string): string | undefined {
    return this.productImages.get(productId);
  }

  getStatusClass(status: OrderStatus): string {
    const statusClasses: Record<OrderStatus, string> = {
      PENDING: 'status-pending',
      READY_FOR_DELIVERY: 'status-ready',
      SHIPPED: 'status-shipped',
      DELIVERED: 'status-delivered',
      CANCELLED: 'status-cancelled',
    };
    return statusClasses[status] || 'status-pending';
  }

  getStatusLabel(status: OrderStatus): string {
    const labels: Record<OrderStatus, string> = {
      PENDING: 'Processing',
      READY_FOR_DELIVERY: 'Ready',
      SHIPPED: 'Shipped',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled',
    };
    return labels[status] || status;
  }

  formatDate(date: Date | string | undefined): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }

  getOrderNumber(order: Order): string {
    if (!order.id) return '';
    const year = order.createdAt
      ? new Date(order.createdAt).getFullYear()
      : new Date().getFullYear();
    return `ORD-${year}-${order.id.slice(-3).toUpperCase()}`;
  }

  canCancel(order: Order): boolean {
    return order.status === 'PENDING';
  }

  cancelOrder(order: Order): void {
    if (!order.id || !confirm('Are you sure you want to cancel this order?')) {
      return;
    }

    this.orderService.cancelOrder(order.id).subscribe({
      next: (updatedOrder) => {
        const index = this.orders.findIndex((o) => o.id === order.id);
        if (index !== -1) {
          this.orders[index] = updatedOrder;
          this.calculateStats();
        }
      },
      error: (err) => {
        console.error('Failed to cancel order:', err);
        alert('Failed to cancel order. Please try again.');
      },
    });
  }
}

