import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'currencyFormat',
  standalone: true
})
export class CurrencyFormatPipe implements PipeTransform {

  private readonly currencyLocales: Record<string, string> = {
    USD: 'en-US',
    GBP: 'en-GB',
    EUR: 'de-DE',
    INR: 'en-IN',
    BRL: 'pt-BR',
    JPY: 'ja-JP',
    AUD: 'en-AU',
    CAD: 'en-CA'
  };

  transform(value: number | null | undefined, currencyCode: string = 'USD'): string {
    if (value === null || value === undefined || isNaN(value)) {
      return '-';
    }

    const code = (currencyCode || 'USD').toUpperCase();
    const locale = this.currencyLocales[code] || 'en-US';

    try {
      const isZeroDecimal = code === 'JPY';
      const formatter = new Intl.NumberFormat(locale, {
        style: 'currency',
        currency: code,
        minimumFractionDigits: isZeroDecimal ? 0 : 2,
        maximumFractionDigits: isZeroDecimal ? 0 : 2
      });

      return formatter.format(value);
    } catch {
      // Fallback if currency code is not standard
      return `${code} ${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    }
  }
}
