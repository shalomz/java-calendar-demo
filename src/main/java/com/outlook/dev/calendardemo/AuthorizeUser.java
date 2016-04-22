package com.outlook.dev.calendardemo;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.outlook.dev.calendardemo.auth.AuthHelper;
import com.outlook.dev.calendardemo.dto.User;

/**
 * Servlet implementation class AuthorizeUser
 */
public class AuthorizeUser extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	// This implements the redirect URL for user login. This is where
	// Azure's login page will redirect the browser after the user logs in and consents.
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Retrieve the saved state and nonce for comparison
		HttpSession session = request.getSession();
		String expectedState = session.getAttribute("auth_state").toString();
		String expectedNonce = session.getAttribute("auth_nonce").toString();
		session.removeAttribute("auth_state");
		session.removeAttribute("auth_nonce");
		
		// Get the state parameter from the request
		String state = request.getParameter("state");
		if (!state.equals(expectedState)) {
			request.setAttribute("error_message", String.format("The state parameter (%s) did not match the expected value (%s)", state, expectedState));
			request.getRequestDispatcher("error.jsp").forward(request, response);
			return;
		}
		else{
			// Check if there is an error
			String error = request.getParameter("error");
			if (error != null && !error.isEmpty()){
				// Get the error description
				String description = request.getParameter("error_description");
				request.setAttribute("error_message", String.format("ERROR: %s - %s", error, description));
				request.getRequestDispatcher("error.jsp").forward(request, response);
				return;
			}
			else{
				// Get the ID token from the request
				String encodedToken = request.getParameter("id_token");
				// Validate the token. If valid it returns the token as a JsonObject
				JsonObject token = AuthHelper.validateUserToken(encodedToken, UUID.fromString(expectedNonce));
				
				if (token != null){
					// Token is valid, we can proceed
					User user = new User(token, false);
					
					// In the user login flow, we also get back an auth code
					String authCode = request.getParameter("code");
					if (authCode != null) {
						// Exchange the auth code for a token and save it in the user object
						user.setTokenObj(AuthHelper.getTokenFromAuthCode(user, request.getRequestURL().toString(), authCode));
						session.setAttribute("user", user);
						response.sendRedirect("Calendars");
						return;
					}
					request.setAttribute("error_message", "No auth code in response");
					request.getRequestDispatcher("error.jsp").forward(request, response);
					return;
				}
				else{
					// Token invalid
					request.setAttribute("error_message", "ID token failed validation");
					request.getRequestDispatcher("error.jsp").forward(request, response);
					return;
				}
			}
		}
	}

}
