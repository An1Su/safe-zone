export interface CartItem {
  id?: string;
  productId: string;
  productName: string;
  sellerId: string; // Seller's user ID or email
  price: number;
  quantity: number;
  stock: number; // Available stock at time of adding to cart
  available?: boolean; // Whether item is currently available (from backend)
  image?: string;
}

export interface Cart {
  userId: string;
  items: CartItem[];
  total: number;
}

export interface CartValidationResult {
  isValid: boolean;
  invalidItems: CartItemValidationError[];
}

export interface CartItemValidationError {
  productId: string;
  productName: string;
  issue: 'out_of_stock' | 'insufficient_stock' | 'not_found';
  currentStock: number;
  requestedQuantity: number;
}
