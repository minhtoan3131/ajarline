import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'

describe('NotFoundPage', () => {
  it('render_ShouldLinkToSeriesIndex_WhenRouteIsUnknown', () => {
    renderAppRoute('/khong-co-trang-nay')

    expect(screen.getByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Xem các Series' })).toHaveAttribute('href', '/series')
  })
})
