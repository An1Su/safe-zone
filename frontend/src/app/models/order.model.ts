export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

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

export interface CreateOrderRequest {
  items: OrderItem[];
  shippingAddress: ShippingAddress;
}

export interface OrderStats {
  totalSpent: number;
  orderCount: number;
  ordersByStatus: Record<OrderStatus, number>;
  mostPurchasedProducts?: { productId: string; productName: string; quantity: number }[];
}

