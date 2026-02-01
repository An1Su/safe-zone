import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Order, OrderItem } from '../../models/order.model';
import { AuthService } from '../../services/auth.service';
import { OrderService } from '../../services/order.service';

interface BuyerStats {
  totalSpent: number;
  mostBoughtProducts: { name: string; quantity: number }[];
}

interface SellerStats {
  revenue: number;
  bestSellingProducts: { name: string; unitsSold: number; revenue: number }[];
  totalUnitsSold: number;
}

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, RouterModule, BaseChartDirective],
  templateUrl: './analytics.html',
  styleUrl: './analytics.scss',
})
export class Analytics implements OnInit {
  loading = true;
  error = '';
  isBuyer = false;
  isSeller = false;

  // Buyer stats
  buyerStats: BuyerStats = {
    totalSpent: 0,
    mostBoughtProducts: [],
  };

  // Seller stats
  sellerStats: SellerStats = {
    revenue: 0,
    bestSellingProducts: [],
    totalUnitsSold: 0,
  };

  // Chart configurations (initialized empty, populated in calculation methods)
  buyerProductChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [],
  };

  sellerProductChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [],
  };

  sellerRevenueChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [],
  };

  // Chart color constants (avoid duplication)
  private readonly CHART_COLORS = {
    buyer: {
      backgroundColor: 'rgba(75, 192, 192, 0.6)',
      borderColor: 'rgba(75, 192, 192, 1)',
    },
    sellerUnits: {
      backgroundColor: 'rgba(255, 99, 132, 0.6)',
      borderColor: 'rgba(255, 99, 132, 1)',
    },
    sellerRevenue: {
      backgroundColor: 'rgba(54, 162, 235, 0.6)',
      borderColor: 'rgba(54, 162, 235, 1)',
    },
  };

  chartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
      },
      tooltip: {
        enabled: true,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
      },
      x: {
        ticks: {
          maxRotation: 45,
          minRotation: 0,
        },
      },
    },
  };

  constructor(
    private orderService: OrderService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isBuyer = this.authService.isClient();
    this.isSeller = this.authService.isSeller();

    if (this.isBuyer) {
      this.loadBuyerAnalytics();
    } else if (this.isSeller) {
      this.loadSellerAnalytics();
    } else {
      this.error = 'Unauthorized access';
      this.loading = false;
    }
  }

  loadBuyerAnalytics(): void {
    this.orderService.getOrders().subscribe({
      next: (orders: Order[]) => {
        const validOrders = this.filterValidOrders(orders);
        this.calculateBuyerStats(validOrders);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading buyer analytics:', err);
        this.error = 'Failed to load analytics';
        this.loading = false;
      },
    });
  }

  loadSellerAnalytics(): void {
    this.orderService.getSellerOrders().subscribe({
      next: (orders: Order[]) => {
        const validOrders = this.filterValidOrders(orders);
        this.calculateSellerStats(validOrders);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading seller analytics:', err);
        this.error = 'Failed to load analytics';
        this.loading = false;
      },
    });
  }

  // Extract common filter logic - only include DELIVERED orders
  private filterValidOrders(orders: Order[]): Order[] {
    return orders.filter((o) => o.status === 'DELIVERED');
  }

  calculateBuyerStats(orders: Order[]): void {
    if (orders.length === 0) {
      this.buyerStats = { totalSpent: 0, mostBoughtProducts: [] };
      return;
    }

    // Calculate total spent
    this.buyerStats.totalSpent = orders.reduce(
      (sum, order) => sum + order.totalAmount,
      0
    );

    // Calculate most bought products
    const productMap = new Map<string, number>();
    orders.forEach((order) => {
      order.items.forEach((item: OrderItem) => {
        const current = productMap.get(item.productName) || 0;
        productMap.set(item.productName, current + item.quantity);
      });
    });

    // Sort by quantity and take top 5
    this.buyerStats.mostBoughtProducts = Array.from(productMap.entries())
      .map(([name, quantity]) => ({ name, quantity }))
      .sort((a, b) => b.quantity - a.quantity)
      .slice(0, 5);

    // Update chart data
    this.buyerProductChartData = {
      labels: this.buyerStats.mostBoughtProducts.map((p) => p.name),
      datasets: [
        {
          data: this.buyerStats.mostBoughtProducts.map((p) => p.quantity),
          label: 'Quantity Purchased',
          backgroundColor: this.CHART_COLORS.buyer.backgroundColor,
          borderColor: this.CHART_COLORS.buyer.borderColor,
          borderWidth: 1,
        },
      ],
    };
  }

  calculateSellerStats(orders: Order[]): void {
    if (orders.length === 0) {
      this.sellerStats = {
        revenue: 0,
        bestSellingProducts: [],
        totalUnitsSold: 0,
      };
      return;
    }

    // Calculate revenue and units sold
    const productMap = new Map<
      string,
      { unitsSold: number; revenue: number }
    >();

    orders.forEach((order) => {
      order.items.forEach((item: OrderItem) => {
        const current = productMap.get(item.productName) || {
          unitsSold: 0,
          revenue: 0,
        };
        productMap.set(item.productName, {
          unitsSold: current.unitsSold + item.quantity,
          revenue: current.revenue + item.price * item.quantity,
        });
      });
    });

    // Calculate total stats
    this.sellerStats.revenue = Array.from(productMap.values()).reduce(
      (sum, p) => sum + p.revenue,
      0
    );
    this.sellerStats.totalUnitsSold = Array.from(productMap.values()).reduce(
      (sum, p) => sum + p.unitsSold,
      0
    );

    // Sort by units sold and take top 5
    this.sellerStats.bestSellingProducts = Array.from(productMap.entries())
      .map(([name, stats]) => ({
        name,
        unitsSold: stats.unitsSold,
        revenue: stats.revenue,
      }))
      .sort((a, b) => b.unitsSold - a.unitsSold)
      .slice(0, 5);

    // Update chart data
    this.sellerProductChartData = {
      labels: this.sellerStats.bestSellingProducts.map((p) => p.name),
      datasets: [
        {
          data: this.sellerStats.bestSellingProducts.map((p) => p.unitsSold),
          label: 'Units Sold',
          backgroundColor: this.CHART_COLORS.sellerUnits.backgroundColor,
          borderColor: this.CHART_COLORS.sellerUnits.borderColor,
          borderWidth: 1,
        },
      ],
    };

    this.sellerRevenueChartData = {
      labels: this.sellerStats.bestSellingProducts.map((p) => p.name),
      datasets: [
        {
          data: this.sellerStats.bestSellingProducts.map((p) => p.revenue),
          label: 'Revenue ($)',
          backgroundColor: this.CHART_COLORS.sellerRevenue.backgroundColor,
          borderColor: this.CHART_COLORS.sellerRevenue.borderColor,
          borderWidth: 1,
        },
      ],
    };
  }

  // Separate reload method to avoid calling ngOnInit recursively
  reload(): void {
    this.loading = true;
    this.error = '';
    this.ngOnInit();
  }
}
