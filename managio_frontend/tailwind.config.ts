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
        sans:    ['var(--font-inter)', 'system-ui', '-apple-system', 'sans-serif'],
        display: ['var(--font-jakarta)', 'var(--font-inter)', 'system-ui', 'sans-serif'],
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
        surface: {
          0:             'hsl(var(--surface-0))',
          1:             'hsl(var(--surface-1))',
          2:             'hsl(var(--surface-2))',
          3:             'hsl(var(--surface-3))',
        },
        primary: {
          DEFAULT:       'hsl(var(--primary))',
          foreground:    'hsl(var(--primary-foreground))',
          hover:         'hsl(var(--primary-hover))',
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
        success: {
          DEFAULT:       'hsl(var(--success))',
          foreground:    'hsl(var(--success-foreground))',
        },
        warning: {
          DEFAULT:       'hsl(var(--warning))',
          foreground:    'hsl(var(--warning-foreground))',
        },
        error: {
          DEFAULT:       'hsl(var(--error))',
          foreground:    'hsl(var(--error-foreground))',
        },
        info: {
          DEFAULT:       'hsl(var(--info))',
          foreground:    'hsl(var(--info-foreground))',
        },
      },
      borderRadius: {
        '2xl': '1rem',
        xl:    'var(--radius)',
        lg:    'calc(var(--radius) - 2px)',
        md:    'calc(var(--radius) - 4px)',
        sm:    'calc(var(--radius) - 6px)',
      },
      animation: {
        'shimmer':       'shimmer 1.8s ease-in-out infinite',
        'fade-in':       'fade-in 0.3s ease-out',
        'slide-up':      'slide-up 0.4s cubic-bezier(0.22,1,0.36,1)',
        'slide-down':    'slide-down 0.4s cubic-bezier(0.22,1,0.36,1)',
        'scale-in':      'scale-in 0.2s ease-out',
        'pulse-glow':    'pulse-glow 2s ease-in-out infinite',
        'float':         'float 3s ease-in-out infinite',
        'gradient-shift':'gradient-shift 8s ease infinite',
        'spin-slow':     'spin-slow 12s linear infinite',
      },
      keyframes: {
        shimmer: {
          '0%':   { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(100%)' },
        },
        'fade-in': {
          from: { opacity: '0' },
          to:   { opacity: '1' },
        },
        'slide-up': {
          from: { opacity: '0', transform: 'translateY(8px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
        'slide-down': {
          from: { opacity: '0', transform: 'translateY(-8px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
        'scale-in': {
          from: { opacity: '0', transform: 'scale(0.95)' },
          to:   { opacity: '1', transform: 'scale(1)' },
        },
        'pulse-glow': {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(99,102,241,0.4)' },
          '50%':      { boxShadow: '0 0 0 8px rgba(99,102,241,0)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%':      { transform: 'translateY(-6px)' },
        },
        'gradient-shift': {
          '0%':   { backgroundPosition: '0% 50%' },
          '50%':  { backgroundPosition: '100% 50%' },
          '100%': { backgroundPosition: '0% 50%' },
        },
        'spin-slow': {
          from: { transform: 'rotate(0deg)' },
          to:   { transform: 'rotate(360deg)' },
        },
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '112': '28rem',
        '128': '32rem',
      },
      transitionTimingFunction: {
        'out-expo': 'cubic-bezier(0.22, 1, 0.36, 1)',
      },
    },
  },
  plugins: [],
}

export default config