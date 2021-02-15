/// <reference types="cypress" />

function hideDevtools() {
  cy
    .get('.panel')
    .invoke('css', 'display', 'none')
    .should('have.css', 'display', 'none')
}

describe('Basic test', () => {
  it('can navigate to offerings', () => {
    cy.findByText('View Offerings').click()
    cy.url().should('equal', 'http://localhost:4544/offerings')
  })

  it.only('can register domain', () => {
    cy.visit('http://localhost:4544/instant-registration')
    hideDevtools()

    cy.findAllByRole('textbox')
      .then(elems => cy.wrap(elems[1]).type('mydomain'))
    cy.findByRole('button', { name: 'Register' }).click()

    cy.findByText('mydomain.eth was successfully registered')
  })
})
