import { CurrencyFormatPipe } from './currency-format.pipe';

describe('CurrencyFormatPipe', () => {
  let pipe: CurrencyFormatPipe;

  beforeEach(() => {
    pipe = new CurrencyFormatPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should format USD currency with 2 decimal places and dollar sign', () => {
    const formatted = pipe.transform(120000.5, 'USD');
    expect(formatted).toContain('120,000.50');
    expect(formatted).toContain('$');
  });

  it('should format GBP currency with pound sign and 2 decimals', () => {
    const formatted = pipe.transform(65000, 'GBP');
    expect(formatted).toContain('65,000.00');
    expect(formatted).toContain('£');
  });

  it('should format INR currency with rupee symbol and subcontinental groupings', () => {
    const formatted = pipe.transform(1500000, 'INR');
    expect(formatted).toContain('15,00,000.00');
    expect(formatted).toContain('₹');
  });

  it('should format JPY currency with ZERO decimal places (edge case)', () => {
    const formatted = pipe.transform(6500000, 'JPY');
    // JPY has no fractional sub-units (0 decimals)
    expect(formatted).toContain('6,500,000');
    expect(formatted).not.toContain('.00');
    expect(formatted).toMatch(/[¥￥]/);
  });

  it('should return hyphen for null, undefined, or NaN values', () => {
    expect(pipe.transform(null)).toBe('-');
    expect(pipe.transform(undefined)).toBe('-');
    expect(pipe.transform(NaN)).toBe('-');
  });
});
