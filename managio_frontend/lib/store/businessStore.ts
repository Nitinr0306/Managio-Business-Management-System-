import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { Business } from '@/lib/types/business'

interface BusinessState {
  currentBusiness: Business | null
  businesses: Business[]
  setCurrentBusiness: (b: Business | null) => void
  setBusinesses: (bs: Business[]) => void
  clearBusinessContext: () => void
}

export const useBusinessStore = create<BusinessState>()(
  persist(
    (set) => ({
      currentBusiness: null,
      businesses: [],
      setCurrentBusiness: (currentBusiness) => set({ currentBusiness }),
      setBusinesses: (businesses) => set({ businesses }),
      clearBusinessContext: () => set({ currentBusiness: null }),
    }),
    {
      name: 'managio-business',
      storage: createJSONStorage(() =>
        typeof window !== 'undefined'
          ? localStorage
          : { getItem: () => null, setItem: () => {}, removeItem: () => {} }
      ),
    }
  )
)