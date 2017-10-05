%define __jar_repack {%nil}
# Compatibility with ncar
%define _source_payload w0.gzdio
%define _binary_payload w0.gzdio
%define _binary_filedigest_algorithm 1

# For use below
%define _prefix %{_usr}/local/chronopolis/replication
%define jar replicationd.jar
%define yaml application.yml
%define service /usr/lib/systemd/system/replicationd.service
%define build_date %(date +"%Y%m%d")

Name: replicationd
Version: %{ver}
Release: %{build_date}.el7
Source: replicationd.service
Source1: replication-shell.jar
Source2: application.yml
Summary: Chronopolis Replication Service
License: UMD
URL: https://gitlab.umiacs.umd.edu/chronopolis
Group: System Environment/Daemons
autoprov: yes
autoreq: yes
BuildArch: noarch
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description

The Replication Services monitors for packages being ingested into Chronopolis
and does replication and registration on them.

%preun

systemctl disable replicationd

%install

%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{service}"
%__install -D -m0644 "%{SOURCE1}" "%{buildroot}%{_prefix}/%{jar}"
%__install -D -m0644 "%{SOURCE2}" "%{buildroot}%{_prefix}/%{yaml}"

%__install -d "%{buildroot}/var/log/chronopolis"

%files

%defattr(-,root,root)
%dir %{_prefix}
%dir %attr(0755,-,-) /var/log/chronopolis

%{service}
%{_prefix}/%{jar}
%config(noreplace) %{_prefix}/%{yaml}

%changelog

* Tue Oct 3 2017 Mike Ritter <shake@umiacs.umd.edu> 1.6.0-20171003
- cleanup spec to include missing sections
