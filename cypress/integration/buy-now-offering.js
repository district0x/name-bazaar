/// <reference types="cypress" />

function findListItemByText(text) {
  return cy.get('.expandable-list-item').filter(`:contains("${text}")`)
}

describe('Buy now offering', () => {
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
      cy.closeTransactionLog()
    })
  })

  it('does not appear in offerings before ownership transfer', () => {
    cy.findByText('Offerings').click()
    cy.findByText(domain).should('not.exist')
  })

  describe('after transfering ownership', () => {
    let url

    beforeEach(() => {
      url = `${domain}.eth`
      cy.findByText('My Offerings').click()
      // verify that it is a buy now offering
      findListItemByText(url).contains('Buy Now')
      cy.findByText('Ownership').click({ force: true })
      cy.findByRole('button', { name: 'Transfer Ownership' }).click({
        force: true,
      })
      cy.closeTransactionLog()
    })

    it('appears in offerings', () => {
      cy.findByText('Offerings').click()
      cy.findByText(url).should('exist')
    })

    it.skip('can be reclaimed back by the auction creation', () => {
      cy.findByText('Reclaim Ownership').click({ force: true })
      cy.findByText('Transfer Ownership').should('exist')

      // transfer ownership again
      cy.findByRole('button', { name: 'Transfer Ownership' }).click({
        force: true,
      })
      cy.closeTransactionLog()
    })

    describe('when bought by another account', () => {
      beforeEach(() => {
        cy.switchAccount(1)
        cy.findByText('Offerings').click()
        cy.findByText(url).click({ force: true })
        cy.findByRole('button', { name: 'Buy' }).click()
        cy.closeTransactionLog()
      })

      it('should be listed in My Purchases for buyer', () => {
        cy.findByText('My Purchases').click()
        cy.findByText(url).should('exist')
      })

      it('will be listed as "Sold" for the previous owner', () => {
        cy.switchAccount(0)
        cy.findByText('My Offerings').click()
        cy.get('.expandable-list-item')
          .filter(`:contains("${url}")`)
          .contains('Sold')
      })
    })
  })
})
