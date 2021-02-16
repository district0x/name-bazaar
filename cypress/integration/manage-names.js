/// <reference types="cypress" />

function getSection(label) {
  return cy.get('.ui.segment').filter(`:contains("${label}")`)
}

function transferDomain(domain) {
  let account

  cy.findByText('Manage Names').click()
  cy.findAllByRole('listbox').eq(0).click()
  cy.findAllByRole('option')
    .eq(1)
    .within(() => {
      cy.get('span').then((elem) => {
        account = elem.text()
      })
    })
    .then(() => {
      getSection('Transfer Ownership').findInputByLabel('Name').type(domain)
      getSection('Transfer Ownership').findInputByLabel('Address').type(account)
      cy.findByRole('button', { name: 'Transfer Ownership' }).click({
        force: true,
      })
      cy.closeTransactionLog()

      // check that the receiver is owner of the domain
      cy.switchAccount(1)
      getSection('Transfer Ownership').contains('You are owner of this name')

      // check that the sender is NOT owner of the domain
      cy.switchAccount(0)
      getSection('Transfer Ownership')
        .contains('You are owner of this name')
        .should('not.exist')
    })
}

describe('Manage names', () => {
  let domain

  beforeEach(() => {
    cy.registerDomain().then((d) => {
      domain = d
    })
  })

  it('can transfer subname to another account', () => {
    const sub = 'subdomain'
    const subdomain = `${sub}.${domain}`

    cy.findByText('Manage Names').click()
    getSection('Create Subname').findInputByLabel('Name').type(domain)
    getSection('Create Subname').findInputByLabel('Subname').type(sub)
    cy.findByRole('button', { name: 'Create Subname' }).click()
    cy.closeTransactionLog()
    transferDomain(subdomain)
  })

  // TODO: fixme
  it.skip('can transfer name to another account', () => {
    transferDomain(domain)
  })
})
