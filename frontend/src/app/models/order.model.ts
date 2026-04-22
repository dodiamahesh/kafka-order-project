// ── Order Model — mirrors the Spring Boot entity ──────────
export type OrderStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface Order {
  id: number;
  customerName: string;
  product: string;
  quantity: number;
  price: number;
  status: OrderStatus;
  statusMessage: string;
  createdAt: string;    // ISO datetime from backend
  processedAt: string | null;
}

export interface OrderRequest {
  customerName: string;
  product: string;
  quantity: number;
  price: number;
}

export interface OrderStats {
  total: number;
  pending: number;
  processing: number;
  completed: number;
  failed: number;
}

// ── UI helpers ──────────────────────────────────────────────
export const STATUS_CONFIG: Record<OrderStatus, { label: string; icon: string; cssClass: string }> = {
  PENDING:    { label: 'Pending',    icon: '⏳', cssClass: 'badge-pending'    },
  PROCESSING: { label: 'Processing', icon: '⚙️', cssClass: 'badge-processing' },
  COMPLETED:  { label: 'Completed',  icon: '✅', cssClass: 'badge-completed'  },
  FAILED:     { label: 'Failed',     icon: '❌', cssClass: 'badge-failed'     },
};

export const SAMPLE_PRODUCTS = [
  { name: 'MacBook Pro 16"',    price: 2499.99 },
  { name: 'iPhone 15 Pro',      price: 999.99  },
  { name: 'Sony WH-1000XM5',   price: 349.99  },
  { name: 'Samsung 4K Monitor', price: 649.99  },
  { name: 'Mechanical Keyboard',price: 179.99  },
  { name: 'Wireless Mouse',     price: 89.99   },
  { name: 'USB-C Hub',          price: 59.99   },
  { name: 'Webcam HD 1080p',    price: 129.99  },
];
