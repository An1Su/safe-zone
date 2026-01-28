export type OrderStatus = 'PENDING' | 'READY_FOR_DELIVERY' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface ShippingAddress {
  fullName: string;
  address: string;
  city: string;
  phone: string;
}

export interface OrderItem {
  productId: string;
  productName: string;
  sellerId: string;
  price: number;
  quantity: number;
}

export interface Order {
  id?: string;
  userId: string;
  items: OrderItem[];
  status: OrderStatus;
  totalAmount: number;
  shippingAddress: ShippingAddress;
  createdAt?: Date;
  updatedAt?: Date;
}

// Backend creates order from cart, so we only need shipping address
export type CreateOrderRequest = ShippingAddress;

export interface OrderStats {
  totalSpent: number;
  orderCount: number;
  ordersByStatus: Record<OrderStatus, number>;
  mostPurchasedProducts?: { productId: string; productName: string; quantity: number }[];
}

export interface OrderSearchParams {
  q?: string; // Search query (order ID, product name, buyer email)
  status?: OrderStatus; // Filter by status
  dateFrom?: string; // Filter from date (ISO format)
  dateTo?: string; // Filter to date (ISO format)
}
