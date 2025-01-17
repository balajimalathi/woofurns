package com.skndan.robin.service.auth;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
 
import org.keycloak.representations.idm.RoleRepresentation;

import com.skndan.robin.config.keycloak.KeycloakService;
import com.skndan.robin.entity.auth.Profile;
import com.skndan.robin.exception.GenericException;
import com.skndan.robin.model.auth.SignUpRequest;
import com.skndan.robin.model.auth.UserRecord;
import com.skndan.robin.repo.auth.ProfileRepo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;

@ApplicationScoped
public class AuthService {

  @Inject
  private KeycloakService keycloakService;

  @Inject
  private ProfileRepo profileRepo;

  public Profile createProfile(SignUpRequest dept) {

    String userId = "";

    if (dept.isSocial()) {
      userId = dept.getUserId();
      String roleId = setDefaultRole(dept);
      return setProfile(dept, userId, roleId);
    } else {
      UserRecord user = new UserRecord(
          "", dept.getEmail(), dept.getEmail(), dept.getFirstName(), dept.getLastName(), dept.getEmail());
      // update keycloak
      userId = keycloakService.createUser(user);
      dept.setUserId(userId);
      String roleId = setDefaultRole(dept);
      return setProfile(dept, userId, roleId);
    }
  }

  private String setDefaultRole(SignUpRequest dept) {

    String defaultRole = "becff04f-e159-4dc1-8b43-13b169d8e482"; // user

    String roleToFilter = dept.getRoleId() != null ? dept.getRoleId() : defaultRole;

    List<RoleRepresentation> clientRoles = keycloakService.getAllRoles();

    List<RoleRepresentation> filteredRoles = clientRoles.stream()
        .filter(role -> role.getId().equals(roleToFilter))
        .collect(Collectors.toList());

    return filteredRoles.get(0).getId();
  }

  public Profile setProfile(SignUpRequest dept, String userId, String roleId) {
    Profile profile = new Profile();
    profile.setFirstName(dept.getFirstName());
    profile.setLastName(dept.getLastName());
    profile.setEmail(dept.getEmail());
    profile.setMobile(dept.getMobile());
    profile.setUserId(userId);

    // save profile
    Optional<Profile> existingProfile = profileRepo.findByUserId(userId);

    // user is already present
    if (existingProfile.isPresent()) {
      Profile _profile = existingProfile.get();
      return _profile;
    } else {
      try {
        // user not present
        Optional<Profile> existingProfile2 = profileRepo.findByEmail(dept.getEmail());

        // but profile is exist with email
        if (existingProfile2.isPresent()) {
          profile.setUserId(userId);
          profile = profileRepo.save(profile);
        } else {
          // but profile is not exists at-all
          profile = profileRepo.save(profile);
        }
      } catch (ConstraintViolationException e) {
        // Assuming the mobile number field is causing the unique constraint violation
        if (e.getMessage() != null && e.getMessage().contains("mobile_number")) {
          throw new GenericException(400, "Mobile number already exists.");
        }
        throw new GenericException(400, "An unknown error occurred while saving the user.");
      }
    }

    return profile;
  }

}
