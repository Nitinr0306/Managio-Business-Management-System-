import type { Config } from 'tailwindcss'

const config: Config = {
  darkMode: 'class',
  content: [
    './app/**/*.{ts,tsx,mdx}',
    './components/**/*.{ts,tsx}',
    './lib/**/*.{ts,tsx}',
  ],
  theme: {
    extend: {
      fontFamily: {
        sans:    ['var(--font-dm-sans)', 'system-ui', 'sans-serif'],
        display: ['var(--font-syne)',    'system-ui', 'sans-serif'],
      },
      fontWeight: {
        '600': '600',
        '700': '700',
        '800': '800',
      },
      colors: {
        border:          'hsl(var(--border))',
        input:           'hsl(var(--input))',
        ring:            'hsl(var(--ring))',
        background:      'hsl(var(--background))',
        foreground:      'hsl(var(--foreground))',
        primary: {
          DEFAULT:       'hsl(var(--primary))',
          foreground:    'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT:       'hsl(var(--secondary))',
          foreground:    'hsl(var(--secondary-foreground))',
        },
        muted: {
          DEFAULT:       'hsl(var(--muted))',
          foreground:    'hsl(var(--muted-foreground))',
        },
        card: {
          DEFAULT:       'hsl(var(--card))',
          foreground:    'hsl(var(--card-foreground))',
        },
        destructive: {
          DEFAULT:       'hsl(var(--destructive))',
          foreground:    'hsl(var(--destructive-foreground))',
        },
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
      },
      animation: {
        shimmer: 'shimmer 1.5s infinite',
      },
      keyframes: {
        shimmer: {
          '0%':   { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(100%)' },
        },
      },
    },
  },
  plugins: [],
}

export default config