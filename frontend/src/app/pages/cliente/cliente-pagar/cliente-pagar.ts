import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';

import { PagoForm } from '../../../components/pago-form/pago-form';
import { Pago, PagoDatos } from '../../../models/pago.model';
import { Reserva } from '../../../models/reserva.model';
import { ClienteService } from '../../../services/cliente.service';
import { mensajeError } from '../../../services/mensaje-error';

@Component({
  selector: 'app-cliente-pagar',
  imports: [PagoForm],
  templateUrl: './cliente-pagar.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientePagar implements OnInit {
  private clienteService = inject(ClienteService);
  private router = inject(Router);

  reservaId = input.required<string>();

  protected readonly reserva = signal<Reserva | null>(null);
  protected readonly pago = signal<Pago | null>(null);
  protected readonly redirigiendo = signal(false);
  protected readonly error = signal('');

  ngOnInit(): void {
    this.clienteService.reserva(+this.reservaId()).subscribe({
      next: (item) => {
        this.reserva.set(item.reserva);
        this.pago.set(item.pago);
      },
      error: (err: HttpErrorResponse) => this.error.set(mensajeError(err)),
    });
  }

  pagar(datos: PagoDatos): void {
    this.error.set('');
    this.clienteService.pagar(datos).subscribe({
      next: () => this.router.navigate(['/cliente/reservas']),
      error: (err: HttpErrorResponse) => this.error.set(mensajeError(err)),
    });
  }

  // Pide el link firmado al backend y lleva al cliente a la página de pago de Wompi
  pagarConWompi(): void {
    this.error.set('');
    this.redirigiendo.set(true);
    this.clienteService.pagarConWompi(+this.reservaId()).subscribe({
      next: (respuesta) => {
        window.location.href = respuesta.url;
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeError(err));
        this.redirigiendo.set(false);
      },
    });
  }

  volver(): void {
    this.router.navigate(['/cliente/reservas']);
  }
}
