'use client'

import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts'

export function PaymentMethodPie({
  stats,
  colors,
}: {
  stats: Array<{ method: string; total: number; percentage: number }>
  colors: string[]
}) {
  return (
    <ResponsiveContainer width={80} height={80}>
      <PieChart>
        <Pie data={stats} cx="50%" cy="50%" innerRadius={25} outerRadius={38} dataKey="total" paddingAngle={3}>
          {stats.map((_, i) => (
            <Cell key={i} fill={colors[i % colors.length]} />
          ))}
        </Pie>
      </PieChart>
    </ResponsiveContainer>
  )
}

