/// <reference types="cypress" />

it('Can navigate to offerings', () => {
  cy.visit('http://localhost:4544')
  cy.hideDevtoolsAndWalletConnectModal()
  cy.findByText('View Offerings').should('be.visible').click()
  cy.url().should('equal', 'http://localhost:4544/offerings')
})

it('can go to offerings directly', () => {
  cy.hideDevtoolsAndWalletConnectModal()
  cy.visit('http://localhost:4544/offerings')
})
