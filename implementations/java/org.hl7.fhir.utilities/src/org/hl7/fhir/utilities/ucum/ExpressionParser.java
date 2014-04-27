/*******************************************************************************
 * Crown Copyright (c) 2006, 2008, Copyright (c) 2006, 2008 Kestral Computing P/L.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Kestral Computing P/L - initial implementation
 *******************************************************************************/

package org.hl7.fhir.utilities.ucum;

public class ExpressionParser {

	private UcumModel model;
	
	/**
	 * @param model
	 */
	public ExpressionParser(UcumModel model) {
		super();
		this.model = model;
	}

	public Term parse(String code) throws Exception {
		Lexer lexer = new Lexer(code);
		return parseTerm(lexer, true);
	}
	
	private Term parseTerm(Lexer lexer, boolean first) throws Exception {
		Term res = new Term();
		if (first && lexer.getType() == TokenType.NONE) {
			res.setComp(new Factor(1));
		} else if (lexer.getType() == TokenType.SOLIDUS) {
			res.setOp(Operator.DIVISION);
			lexer.consume();
			res.setTerm(parseTerm(lexer, false));
		} else {
			res.setComp(parseComp(lexer));
			if (lexer.getType() != TokenType.NONE) {
				if (lexer.getType() == TokenType.SOLIDUS)
					res.setOp(Operator.DIVISION);
				else if (lexer.getType() == TokenType.PERIOD)
					res.setOp(Operator.MULTIPLICATION);
				else
					lexer.error("Expected '/' or '.'");
				lexer.consume();
				res.setTerm(parseTerm(lexer, false));
			}
		} 
		return res;
	}

	private Component parseComp(Lexer lexer) throws Exception {
		if (lexer.getType() == TokenType.NUMBER) { 
			Factor fact = new Factor(lexer.getTokenAsInt());
			lexer.consume();
			return fact;
		} else if (lexer.getType() == TokenType.SYMBOL)
			return parseSymbol(lexer);
		else if  (lexer.getType() == TokenType.NONE)
			lexer.error("unexpected end of expression looking for a symbol or a number");
		else
			lexer.error("unexpected token looking for a symbol or a number");
		return null; // we never get to here
	}

	private Component parseSymbol(Lexer lexer) throws Exception {
		Symbol symbol = new Symbol(); 
		String sym = lexer.getToken();
		
		// now, can we pick a prefix that leaves behind a metric unit?
		Prefix selected = null;
		Unit unit = null;
		for (Prefix prefix : model.getPrefixes()) {
			if (sym.startsWith(prefix.getCode())) {
				unit = model.getUnit(sym.substring(prefix.getCode().length()));
				if (unit != null && (unit.getKind() == ConceptKind.BASEUNIT || ((DefinedUnit) unit).isMetric())) {
					selected = prefix;
					break;
				};				
			}
		}

		if (selected != null) {
			symbol.setPrefix(selected);
			symbol.setUnit(unit);
		} else {
			unit = model.getUnit(sym);
			if (unit != null) 
				symbol.setUnit(unit);
			else if (!sym.equals("1"))
				lexer.error("The unit '"+sym+"' is unknown");
		}
		
		lexer.consume();
		if (lexer.getType() == TokenType.NUMBER) {
			symbol.setExponent(lexer.getTokenAsInt());
			lexer.consume();
		} else
			symbol.setExponent(1);

		return symbol;
	}
}
