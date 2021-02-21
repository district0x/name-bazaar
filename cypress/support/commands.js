// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })

import '@testing-library/cypress/add-commands'

// There are some non breaking errors in the app which make cypress test fail
// TODO: fix those errors and remove this handler
Cypress.on('uncaught:exception', (_err, _runnable) => {
  // returning false here prevents Cypress from failing the test
  return false
})

Cypress.Commands.add(
  'findInputByLabel',
  { prevSubject: true },
  (chain, label) => {
    // semantic-ui uses 'div' to display the label and testing library only allows 'label'
    return cy.wrap(chain).find(`div.ui.label:contains("${label}") + input`)
  },
)

Cypress.Commands.add('getInputByLabel', (label) =>
  // semantic-ui uses 'div' to display the label and testing library only allows 'label'
  cy.get(`div.ui.label:contains("${label}") + input`),
)

Cypress.Commands.add('hideDevtools', () => {
  cy.get('.panel')
    .invoke('css', 'display', 'none')
    .should('have.css', 'display', 'none')
})

Cypress.Commands.add('closeTransactionLog', () => {
  cy.findByText('TRANSACTION LOG').should('be.visible')
  cy.get('.transaction')
    .first()
    .within(() => {
      cy.get('.transaction-status.success').should('exist')
    })
  cy.findByText('TRANSACTION LOG').click()
})

Cypress.Commands.add('switchAccount', (n) => {
  cy.findAllByRole('listbox').eq(0).click()
  cy.findAllByRole('option').eq(n).click()
})

Cypress.Commands.add('registerDomain', () => {
  cy.visit('http://localhost:4544/instant-registration')
  cy.hideDevtools()

  // TODO: make this deterministic (e.g. use fixture of english nouns)
  const domain = Math.random().toString(36).substr(2, 10)

  // sometimes letters are skipped when typing shortly after page loads
  cy.getInputByLabel('Name').type(domain, { delay: 100 })
  // fail early in such that case
  cy.getInputByLabel('Name').should('have.value', domain)

  cy.findByRole('button', { name: 'Register' }).click()
  cy.closeTransactionLog()

  return cy.wrap(domain)
})
