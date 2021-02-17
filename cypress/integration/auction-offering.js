/// <reference types="cypress" />

function findListItemByText(text) {
  return cy
    .get('.expandable-list-item')
    .filter(`:contains("${text}")`)
    .should('be.visible')
}

describe('Auction offering', () => {
  let domain

  before(() => {
    cy.registerDomain().then((d) => {
      domain = d

      cy.findByText('Create Offering').click()
      cy.getInputByLabel('Name').type(domain)
      cy.contains('You are owner of this name')
      cy.findByRole('button', { name: 'Create Offering' }).click()
      cy.findByText(`Offering for ${domain}.eth is ready!`)

      // change auction date
      cy.get('.react-datepicker__input-container').click()
      cy.get('.react-datepicker__day--today').click()
      cy.get('.offering-form').click()
      cy.closeTransactionLog()
    })
  })

  it('does not appear in offerings before ownership transfer', () => {
    cy.findByText('Offerings').click()
    cy.findByText(domain).should('not.exist')
  })

  describe('after transfering ownership', () => {
    let url

    before(() => {
      url = `${domain}.eth`
      cy.findByText('My Offerings').click()
      // verify that it is an auction
      findListItemByText(url).contains('Auction')
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

    describe('other accounts can bid for the offering', () => {
      before(() => {
        cy.switchAccount(1)
        cy.findByText('Offerings').click()
        findListItemByText(url).click()
        cy.findByRole('button', { name: 'Bid Now' })
          .should('be.visible')
          .click({ force: true })
        cy.closeTransactionLog()
        cy.contains('Your bid is winning this auction!')

        // another account may overbid
        cy.switchAccount(2)
        cy.get('.bid-section input').clear().type('1.7')
        cy.findByRole('button', { name: 'Bid Now' })
          .should('be.visible')
          .click({ force: true })
        cy.closeTransactionLog()
        cy.contains('Your bid is winning this auction!')

        // previous account is not an owner
        cy.switchAccount(1)
        cy.contains('Your bid is winning this auction!').should('not.exist')
      })

      it('correctly displays My Bids section', () => {
        cy.switchAccount(0)
        findListItemByText(url).within(() => {
          cy.get(':nth-child(3)').contains('2') // total bids
          cy.get(':nth-child(5)').contains('1.7') // highest bid
        })

        cy.switchAccount(1)
        findListItemByText(url)
          .contains('Your bid is winning this auction!')
          .should('not.exist')

        cy.switchAccount(2)
        findListItemByText(url)
          .contains('Your bid is winning this auction!')
          .should('exist')
      })

      // TODO: test auction finalization
    })
  })
})
