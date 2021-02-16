/// <reference types="cypress" />

it('Can navigate to offerings', () => {
  cy.visit('http://localhost:4544')
  cy.findByText('View Offerings').click()
  cy.url().should('equal', 'http://localhost:4544/offerings')
})

it('can go to offerings directly', () => {
  cy.visit('http://localhost:4544/offerings')
})
