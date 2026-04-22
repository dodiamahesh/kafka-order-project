import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../services/order.service';
import { SAMPLE_PRODUCTS } from '../../models/order.model';

@Component({
  selector: 'app-order-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './order-form.component.html',
  styleUrls: ['./order-form.component.css']
})
export class OrderFormComponent {

  private fb = inject(FormBuilder);
  private orderService = inject(OrderService);

  orderPlaced = output<void>();

  sampleProducts = SAMPLE_PRODUCTS;
  isSubmitting = signal(false);
  submitSuccess = signal(false);
  errorMessage = signal('');

  form = this.fb.group({
    customerName: ['', [Validators.required, Validators.minLength(2)]],
    product:      ['', [Validators.required]],
    quantity:     [1, [Validators.required, Validators.min(1), Validators.max(1000)]],
    price:        [null as number | null, [Validators.required, Validators.min(0.01)]]
  });

  // Auto-fill price when a sample product is selected
  onProductChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    const found = this.sampleProducts.find(p => p.name === val);
    if (found) {
      this.form.patchValue({ price: found.price });
    }
  }

  // Fill with random sample data (for quick demo)
  fillDemo(): void {
    const names = ['Ravi Sharma', 'Priya Patel', 'Arjun Mehta', 'Sneha Joshi', 'Vikram Singh'];
    const product = this.sampleProducts[Math.floor(Math.random() * this.sampleProducts.length)];
    this.form.setValue({
      customerName: names[Math.floor(Math.random() * names.length)],
      product: product.name,
      quantity: Math.floor(Math.random() * 5) + 1,
      price: product.price
    });
  }

  submit(): void {
    if (this.form.invalid || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const request = {
      customerName: this.form.value.customerName!,
      product:      this.form.value.product!,
      quantity:     this.form.value.quantity!,
      price:        this.form.value.price!,
    };

    this.orderService.placeOrder(request).subscribe({
      next: (order) => {
        this.isSubmitting.set(false);
        this.submitSuccess.set(true);
        this.form.reset({ quantity: 1 });
        setTimeout(() => this.submitSuccess.set(false), 3000);
        this.orderPlaced.emit();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to place order. Is the backend running?');
      }
    });
  }

  get f() { return this.form.controls; }
}
