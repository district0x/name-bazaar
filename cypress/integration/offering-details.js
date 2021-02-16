/// <reference types="cypress" />

describe('Offering details', () => {
  let domain

  beforeEach(() => {
    cy.registerDomain().then((d) => {
      domain = d

      cy.findByText('Create Offering').click()
      cy.getInputByLabel('Name').type(domain)
      cy.contains('You are owner of this name')
      cy.findAllByRole('listbox').eq(1).click()
      cy.findByText('Buy Now').click({ force: true })
      cy.findByRole('button', { name: 'Create Offering' }).click()
      cy.findByText(`Offering for ${domain}.eth is ready!`)
      cy.findByRole('button', { name: 'Show Me' }).click()
      cy.closeTransactionLog()
    })
  })

  it('can transfer ownership', () => {
    cy.contains('Missing Ownership')
    cy.findByRole('button', { name: 'Transfer Ownership' }).click({
      force: true,
    })
    cy.contains('Active')
    cy.findByText('button', { name: 'Delete' }).should('not.exist')
  })

  it('can remove the offering', () => {
    cy.findByRole('button', { name: 'Delete' }).click()
    cy.contains('This offering was deleted by original owner.')
  })
})
